package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.autobuilder.AutoBuilder;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import static javax.lang.model.util.ElementFilter.typesIn;
import static javax.tools.Diagnostic.Kind.ERROR;
import static net.autobuilder.core.LessElements.asType;

public final class Processor extends AbstractProcessor {

  private static final String SUFFIX = "_Builder";

  private final Set<TypeName> done = new HashSet<>();

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return new HashSet<>();
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
    Set<TypeElement> typeElements =
        typesIn(env.getElementsAnnotatedWith(AutoBuilder.class));
    validate(typeElements);
    for (TypeElement typeElement : typeElements) {
      try {
        Model model = Model.create(typeElement);
        if (!done.add(model.source)) {
          continue;
        }
        TypeSpec typeSpec = Analyser.create(model).analyse();
        write(model.generatedClass, typeSpec);
      } catch (ValidationException e) {
        processingEnv.getMessager().printMessage(e.kind, e.getMessage(), e.about);
      } catch (Exception e) {
        handleException(typeElement, e);
        return false;
      }
    }
    return false;
  }

  private void handleException(TypeElement typeElement, Exception e) {
    e.printStackTrace();
    String message = "Error processing " +
        ClassName.get(asType(typeElement.getEnclosingElement())) +
        ": " + e.getMessage();
    processingEnv.getMessager().printMessage(ERROR, message, typeElement);
  }

  private void validate(Set<TypeElement> types) {
  }

  private void write(ClassName generatedType, TypeSpec typeSpec) throws IOException {
    JavaFile javaFile = JavaFile.builder(generatedType.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
    JavaFileObject sourceFile = processingEnv.getFiler()
        .createSourceFile(generatedType.toString(),
            javaFile.typeSpec.originatingElements.toArray(new Element[0]));
    try (Writer writer = sourceFile.openWriter()) {
      writer.write(javaFile.toString());
    }
  }

  static final class Model {
    final ClassName source;
    final ClassName generatedClass;

    Model(ClassName source, ClassName generatedClass) {
      this.source = source;
      this.generatedClass = generatedClass;
    }

    static Model create(TypeElement typeElement) {
      ClassName source = ClassName.get(typeElement);
      return new Model(source, peer(source, SUFFIX));
    }
  }

  private static ClassName peer(ClassName type, String suffix) {
    String name = String.join("_", type.simpleNames()) + suffix;
    return type.topLevelClassName().peerClass(name);
  }
}
