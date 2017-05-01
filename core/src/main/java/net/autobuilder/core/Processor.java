package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.autobuilder.AutoBuilder;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static javax.lang.model.util.ElementFilter.typesIn;
import static javax.tools.Diagnostic.Kind.ERROR;

public final class Processor extends AbstractProcessor {

  private static final String PREFIX = "AutoValue_";

  private final Set<TypeName> done = new HashSet<>();
  private final Set<TypeElement> seen = new HashSet<>();

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    Set<String> strings = new HashSet<>();
    strings.add(AutoBuilder.class.getCanonicalName());
    return strings;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
    Set<TypeElement> typeElements =
        typesIn(env.getElementsAnnotatedWith(AutoBuilder.class));
    for (TypeElement typeElement : seen) {
      TypeName sourceClass = TypeName.get(typeElement.asType());
      TypeElement avType = processingEnv.getElementUtils().getTypeElement(
          avPeer(sourceClass).toString());
      if (avType == null) {
        // auto-value isn't finished yet, skip this round
        continue;
      }
      processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "exist");
      Model model = Model.create(sourceClass, avType);
      if (!done.add(model.sourceClass)) {
        continue;
      }
      try {
        TypeSpec typeSpec = Analyser.create(model).analyse();
        write(rawType(model.generatedClass), typeSpec);
      } catch (ValidationException e) {
        processingEnv.getMessager().printMessage(ERROR, e.getMessage(), e.about);
      } catch (Exception e) {
        handleException(typeElement, e);
        return false;
      }
    }
    seen.addAll(typeElements);
    return false;
  }

  private void handleException(TypeElement typeElement, Exception e) {
    e.printStackTrace();
    String message = "Error processing " +
        ClassName.get(typeElement) +
        ": " + e.getMessage();
    processingEnv.getMessager().printMessage(ERROR, message, typeElement);
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

  private static ClassName avPeer(TypeName type) {
    String name = PREFIX + String.join("_", rawType(type).simpleNames());
    return rawType(type).topLevelClassName().peerClass(name);
  }

  static ClassName rawType(TypeName typeName) {
    if (typeName instanceof ParameterizedTypeName) {
      return ((ParameterizedTypeName) typeName).rawType;
    }
    return ((ClassName) typeName);
  }

  static List<TypeName> typeArguments(TypeName typeName) {
    if (typeName instanceof ParameterizedTypeName) {
      return ((ParameterizedTypeName) typeName).typeArguments;
    }
    return Collections.emptyList();
  }
}
