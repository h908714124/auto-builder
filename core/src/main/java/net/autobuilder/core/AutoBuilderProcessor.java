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
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
      TypeElement avType = processingEnv.getElementUtils().getTypeElement(
          avPeer(sourceClass).toString());
      if (avType == null) {
        // auto-value isn't finished yet, skip this round
        writeDummy(sourceClassElement);
        continue;
      }
      try {
        Model model = Model.create(sourceClassElement, avType);
        if (!done.add(model.sourceClass)) {
          continue;
        }
        TypeSpec typeSpec = Analyser.create(model).analyse();
        write(rawType(model.generatedClass), typeSpec);
      } catch (ValidationException e) {
        processingEnv.getMessager().printMessage(e.kind, e.getMessage(), e.about);
      } catch (Exception e) {
        handleException(sourceClassElement, e);
        return false;
      }
    }
    seen.addAll(typeElements);
    return false;
  }

  private void writeDummy(TypeElement sourceClassElement) {
    TypeName sourceClass = TypeName.get(sourceClassElement.asType());
    TypeName generatedClass = Model.abPeer(sourceClass);
    TypeSpec typeSpec = TypeSpec.classBuilder(rawType(generatedClass))
        .build();
    try {
      write(rawType(generatedClass), typeSpec);
    } catch (IOException e) {
      e.printStackTrace();
    }
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
