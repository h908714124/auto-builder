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
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import static javax.lang.model.util.ElementFilter.typesIn;
import static javax.tools.Diagnostic.Kind.ERROR;
import static net.autobuilder.core.LessElements.asType;

public final class Processor extends AbstractProcessor {

  private static final String AB_PREFIX = "AutoBuilder_";
  private static final String AV_PREFIX = "AutoValue_";

  private final Set<TypeName> done = new HashSet<>();
  private final Set<TypeElement> remembered = new HashSet<>();

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    HashSet<String> strings = new HashSet<>();
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
    for (TypeElement typeElement : remembered) {
      ClassName source = ClassName.get(typeElement);
      TypeElement avType = processingEnv.getElementUtils().getTypeElement(
          avPeer(source).toString());
      if (avType == null) {
        // auto-value isn't finished yet, skip this round
        continue;
      }
      processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "exist");
      Model model = Model.create(source, avType);
      if (!done.add(model.source)) {
        continue;
      }
      TypeSpec typeSpec = Analyser.create(model).analyse();
      try {
        write(model.generatedClass, typeSpec);
      } catch (ValidationException e) {
        processingEnv.getMessager().printMessage(e.kind, e.getMessage(), e.about);
      } catch (Exception e) {
        handleException(typeElement, e);
        return false;
      }
    }
    remembered.addAll(typeElements);
    return false;
  }

  private void handleException(TypeElement typeElement, Exception e) {
    e.printStackTrace();
    String message = "Error processing " +
        ClassName.get(asType(typeElement.getEnclosingElement())) +
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

  static final class Model {
    final ClassName source;
    final ClassName generatedClass;
    final TypeElement avType;

    Model(ClassName source, ClassName generatedClass, TypeElement avType) {
      this.source = source;
      this.generatedClass = generatedClass;
      this.avType = avType;
    }

    static Model create(ClassName source, TypeElement avType) {
      return new Model(source, abPeer(source), avType);
    }
  }

  private static ClassName abPeer(ClassName type) {
    String name = AB_PREFIX + String.join("_", type.simpleNames());
    return type.topLevelClassName().peerClass(name);
  }

  private static ClassName avPeer(ClassName type) {
    String name = AV_PREFIX + String.join("_", type.simpleNames());
    return type.topLevelClassName().peerClass(name);
  }
}
