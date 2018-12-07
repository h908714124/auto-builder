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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static javax.lang.model.util.ElementFilter.typesIn;
import static javax.tools.Diagnostic.Kind.ERROR;

public final class AutoBuilderProcessor extends AbstractProcessor {

  private static final String AV_PREFIX = "AutoValue_";

  private final Set<String> deferredTypeNames = new HashSet<>();
  private final Set<String> done = new HashSet<>();

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
    List<TypeElement> deferredTypes = deferredTypeNames.stream()
        .map(name -> processingEnv.getElementUtils().getTypeElement(name))
        .collect(toList());
    if (env.processingOver()) {
      for (TypeElement type : deferredTypes) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
            "Could not find " + avPeer(type) +
                ", maybe auto-value is not configured?", type);
      }
      return false;
    }
    List<TypeElement> types = Stream.of(
        deferredTypes,
        typesIn(env.getElementsAnnotatedWith(AutoBuilder.class)))
        .map(Collection::stream)
        .flatMap(Function.identity())
        .collect(toList());
    deferredTypeNames.clear();
    Util util = new Util(processingEnv);

    for (TypeElement sourceClassElement : types) {
      String key = sourceClassElement.getQualifiedName().toString();
      if (done.contains(key)) {
        continue;
      }
      ClassName generatedByAutoValue = avPeer(sourceClassElement);
      TypeElement avType = util.typeElement(generatedByAutoValue);
      if (avType == null) {
        // Auto-value hasn't written its class yet.
        // Remember this, so we can notify the user later on.
        deferredTypeNames.add(sourceClassElement.getQualifiedName().toString());
        continue;
      }
      try {
        TypeTool.init(processingEnv.getTypeUtils(), processingEnv.getElementUtils());
        Model model = Model.create(util, sourceClassElement, avType);
        TypeSpec typeSpec = Analyser.create(model).analyse();
        write(rawType(model.generatedClass), typeSpec);
        done.add(key);
      } catch (ValidationException e) {
        processingEnv.getMessager().printMessage(e.kind, e.getMessage(), e.about);
      } catch (Exception e) {
        String trace = getStackTraceAsString(e);
        String message = "Unexpected error: " + trace;
        processingEnv.getMessager().printMessage(ERROR, message);
      } finally {
        TypeTool.clear();
      }
    }
    return false;
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
      writer.write(javaFile.toString());
    }
  }

  private static ClassName avPeer(TypeElement typeElement) {
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
