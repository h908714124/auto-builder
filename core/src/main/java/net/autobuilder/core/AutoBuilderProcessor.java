package net.autobuilder.core;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.autobuilder.AutoBuilder;

import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.util.ElementFilter.typesIn;
import static javax.tools.Diagnostic.Kind.ERROR;

public final class AutoBuilderProcessor extends AbstractProcessor {

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
    for (TypeElement sourceClassElement : seen) {
      TypeName sourceClass = TypeName.get(sourceClassElement.asType());
      if (done.contains(sourceClass)) {
        continue;
      }
      ClassName generatedByAutoValue = avPeer(sourceClass);
      TypeElement avType = processingEnv.getElementUtils().getTypeElement(
          generatedByAutoValue.toString());
      if (avType == null) {
        // Auto-value hasn't written its class yet.
        // Leave a placeholder, to notify the user.
        // This will hopefully be overwritten in a future round.
        writePlaceholder(sourceClassElement, generatedByAutoValue);
        continue;
      }
      done.add(sourceClass);
      try {
        Model model = Model.create(sourceClassElement, avType);
        TypeSpec typeSpec = Analyser.create(model).analyse();
        write(sourceClassElement, rawType(model.generatedClass), typeSpec);
      } catch (ValidationException e) {
        processingEnv.getMessager().printMessage(e.kind, e.getMessage(), e.about);
      } catch (Exception e) {
        handleException(sourceClassElement, e);
        return false;
      }
    }
    // Don't even try to do anything in the first round.
    // Just remember these type elements, so we can handle them later.
    seen.addAll(typeElements);
    return false;
  }

  private void writePlaceholder(TypeElement sourceClassElement,
                                ClassName generatedByAutoValue) {
    TypeName sourceClass = TypeName.get(sourceClassElement.asType());
    TypeName generatedClass = Model.abPeer(sourceClass);
    TypeSpec typeSpec = TypeSpec.classBuilder(rawType(generatedClass))
        .addModifiers(Modifier.ABSTRACT)
        .addAnnotation(AnnotationSpec.builder(Generated.class)
            .addMember("value", "$S", AutoBuilderProcessor.class.getCanonicalName())
            .build())
        .addMethod(MethodSpec.methodBuilder("builder")
            .addModifiers(STATIC, PRIVATE)
            .returns(generatedClass)
            .addStatement("throw new $T(\n$S + \n$S)", UnsupportedOperationException.class,
                generatedByAutoValue.simpleName() + " not found. ",
                "Maybe auto-value is not configured?")
            .build())
        .build();
    write(sourceClassElement, rawType(generatedClass), typeSpec);
  }

  private void handleException(TypeElement typeElement, Exception e) {
    String message = "Error processing " +
        ClassName.get(typeElement) +
        ": " + e.getMessage();
    processingEnv.getMessager().printMessage(ERROR, message, typeElement);
  }

  private void write(TypeElement sourceTypeElement,
                     ClassName generatedType, TypeSpec typeSpec) {
    JavaFile javaFile = JavaFile.builder(generatedType.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
    JavaFileObject sourceFile;
    try {
      sourceFile = processingEnv.getFiler()
          .createSourceFile(generatedType.toString(),
              javaFile.typeSpec.originatingElements.toArray(new Element[0]));
    } catch (IOException e) {
      handleException(sourceTypeElement, e);
      return;
    }
    try (Writer writer = sourceFile.openWriter()) {
      writer.write(javaFile.toString());
    } catch (IOException e) {
      handleException(sourceTypeElement, e);
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
