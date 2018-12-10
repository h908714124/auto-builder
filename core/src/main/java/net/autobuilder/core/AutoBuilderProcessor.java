package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.autobuilder.AutoBuilder;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.util.ElementFilter.typesIn;
import static javax.tools.Diagnostic.Kind.ERROR;

public final class AutoBuilderProcessor extends AbstractProcessor {

  private static final String AV_PREFIX = "AutoValue_";

  private final Set<Task> deferred = new HashSet<>();

  private final Set<String> done = new HashSet<>();

  private final boolean debug;

  public AutoBuilderProcessor() {
    this(false);
  }

  // visible for testing
  AutoBuilderProcessor(boolean debug) {
    this.debug = debug;
  }

  private static final class Task {

    final String sourceType;

    final String avType;

    Task(String sourceType, String avType) {
      this.sourceType = sourceType;
      this.avType = avType;
    }

    static Task create(TypeElement sourceClassElement, ClassName avClass) {
      return new Task(sourceClassElement.getQualifiedName().toString(),
          avClass.packageName() + '.' + avClass.simpleName());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Task task = (Task) o;
      return avType.equals(task.avType);
    }

    @Override
    public int hashCode() {
      return Objects.hash(avType);
    }
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Collections.singleton(AutoBuilder.class.getCanonicalName());
  }

  @Override
  public Set<String> getSupportedOptions() {
    // Marking it as aggregating may or may not be necessary,
    // but it should be safer since we're looking at the class that's generated
    // by auto-value, which may violate one rule of "isolating" annotation processing.
    // See here:
    // https://docs.gradle.org/5.0/userguide/java_plugin.html#sec:incremental_annotation_processing
    return super.getSupportedOptions();
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
    if (env.processingOver()) {
      checkUnprocessed();
      return false;
    }
    try {
      TypeTool.init(processingEnv.getTypeUtils(), processingEnv.getElementUtils());
      doProcess(env);
    } finally {
      TypeTool.clear();
    }
    return false;
  }

  private void checkUnprocessed() {
    for (Task task : deferred) {
      if (!done.contains(task.avType)) {
        TypeElement unprocessedType = processingEnv.getElementUtils().getTypeElement(task.sourceType);
        processingEnv.getMessager().printMessage(
            Diagnostic.Kind.ERROR,
            String.format("Could not find %s, maybe auto-value is not configured?", task.avType),
            unprocessedType);
      }
    }
  }

  private void doProcess(RoundEnvironment env) {
    for (Task task : deferred) {
      if (!done.contains(task.avType)) {
        processTask(task);
      }
    }
    for (TypeElement sourceClassElement : typesIn(env.getElementsAnnotatedWith(AutoBuilder.class))) {
      ClassName avClass = avClass(sourceClassElement);
      Task task = Task.create(sourceClassElement, avClass);
      processTask(task);
    }
  }

  private void processTask(Task task) {
    TypeElement sourceElement = processingEnv.getElementUtils().getTypeElement(task.sourceType);
    try {
      TypeElement avElement = processingEnv.getElementUtils().getTypeElement(task.avType);
      if (avElement == null) {
        // Auto-value hasn't written its class yet.
        // Remember this, so we can notify the user later on.
        deferred.add(task);
        return;
      }
      List<Parameter> parameters = RegularParameter.scan(
          Model.generatedClass(sourceElement),
          sourceElement.getModifiers().contains(PUBLIC),
          Model.getAvConstructor(sourceElement, avElement),
          avElement);
      Model model = Model.create(parameters, sourceElement, avElement);

      TypeSpec typeSpec = Builder.create(model).define();
      write(rawType(model.generatedClass), typeSpec);
      done.add(task.avType);
    } catch (ValidationException e) {
      processingEnv.getMessager().printMessage(e.kind, e.getMessage(), e.about);
    } catch (Exception | AssertionError e) {
      String trace = getStackTraceAsString(e);
      String message = "Unexpected error: " + trace;
      processingEnv.getMessager().printMessage(ERROR, message, sourceElement);
    }
  }

  private void write(ClassName generatedType, TypeSpec typeSpec) throws IOException {
    JavaFile javaFile = JavaFile.builder(generatedType.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
    JavaFileObject sourceFile;
    sourceFile = processingEnv.getFiler()
        .createSourceFile(generatedType.toString(),
            javaFile.typeSpec.originatingElements.toArray(new Element[0]));
    try (Writer writer = sourceFile.openWriter()) {
      String sourceCode = javaFile.toString();
      writer.write(sourceCode);
      if (debug) {
        System.err.println("##############");
        System.err.println("# Debug info #");
        System.err.println("##############");
        System.err.println(sourceCode);
      }
    }
  }

  private static ClassName avClass(TypeElement typeElement) {
    ClassName className = ClassName.get(typeElement);
    String name = AV_PREFIX + String.join("_", className.simpleNames());
    return className.topLevelClassName().peerClass(name);
  }

  static ClassName rawType(TypeName typeName) {
    if (typeName instanceof TypeVariableName) {
      return TypeName.OBJECT;
    }
    if (typeName.getClass().equals(TypeName.class)) {
      return TypeName.OBJECT;
    }
    if (typeName instanceof ParameterizedTypeName) {
      return ((ParameterizedTypeName) typeName).rawType;
    }
    return ((ClassName) typeName);
  }

  private static String getStackTraceAsString(Throwable throwable) {
    StringWriter stringWriter = new StringWriter();
    throwable.printStackTrace(new PrintWriter(stringWriter));
    return stringWriter.toString();
  }
}
