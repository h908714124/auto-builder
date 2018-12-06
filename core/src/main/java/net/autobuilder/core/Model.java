package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import net.autobuilder.AutoBuilder;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.Arrays;
import java.util.List;

import static javax.lang.model.element.Modifier.PUBLIC;
import static net.autobuilder.core.AutoBuilderProcessor.rawType;

public final class Model {

  private static final String SUFFIX = "_Builder";

  private final TypeElement sourceClassElement;

  private final ExecutableElement constructor;

  final boolean reuse;

  final TypeName generatedClass;
  final TypeElement avType;
  private final TypeElement sourceClass;
  final Util util;


  private Model(Util util, TypeElement sourceClassElement,
                TypeName generatedClass,
                TypeElement avType,
                boolean reuse,
                ExecutableElement constructor) {
    this.util = util;
    this.sourceClassElement = sourceClassElement;
    this.generatedClass = generatedClass;
    this.avType = avType;
    this.reuse = reuse;
    this.sourceClass = sourceClassElement;
    this.constructor = constructor;
  }

  static Model create(
      Util util,
      TypeElement sourceClassElement, TypeElement avType) {
    List<ExecutableElement> constructors = ElementFilter.constructorsIn(
        avType.getEnclosedElements());
    if (constructors.size() != 1) {
      throw new ValidationException(
          "Expecting the generated auto-value class to have exactly one constructor.",
          sourceClassElement);
    }
    ExecutableElement constructor = constructors.get(0);
    if (constructor.getModifiers().contains(Modifier.PRIVATE)) {
      boolean suspicious = ElementFilter.typesIn(sourceClassElement.getEnclosedElements())
          .stream()
          .anyMatch(
              e -> e.getAnnotationMirrors().stream().anyMatch(annotationMirror -> {
                ClassName className = rawType(TypeName.get(annotationMirror.getAnnotationType()));
                return className.packageName().equals("com.google.auto.value") &&
                    className.simpleNames().equals(Arrays.asList("AutoValue", "Builder"));
              }));
      if (suspicious) {
        throw new ValidationException(
            sourceClassElement + ": @AutoBuilder and @AutoValue.Builder cannot be used together.",
            sourceClassElement);
      }
      throw new ValidationException(
          "Expecting the generated auto-value class to have a non-private constructor.",
          sourceClassElement);
    }
    TypeName generatedClass = generatedClass(sourceClassElement);
    if (!sourceClassElement.getTypeParameters().isEmpty()) {
      throw new ValidationException("The class may not have type parameters.",
          sourceClassElement);
    }
    boolean optionalRefTrackingBuilderClass =
        sourceClassElement.getAnnotation(AutoBuilder.class).reuseBuilder();
    return new Model(util, sourceClassElement, generatedClass, avType,
        optionalRefTrackingBuilderClass, constructor);
  }

  List<ParaParameter> scan() {
    return Parameter.scan(this, constructor, avType);
  }

  private static TypeName generatedClass(TypeElement typeElement) {
    String name = String.join("_", ClassName.get(typeElement).simpleNames()) + SUFFIX;
    return ClassName.get(typeElement).topLevelClassName().peerClass(name);
  }

  static TypeName withTypevars(ClassName className, TypeName[] typevars) {
    if (typevars.length == 0) {
      return className;
    }
    return ParameterizedTypeName.get(className, typevars);
  }

  private boolean isPublic() {
    return sourceClassElement.getModifiers().contains(PUBLIC);
  }

  Modifier[] maybePublic() {
    if (isPublic()) {
      return new Modifier[]{PUBLIC};
    }
    return new Modifier[]{};
  }

  TypeElement sourceClass() {
    return sourceClass;
  }

  ClassName perThreadFactoryClass() {
    return rawType(generatedClass)
        .nestedClass("PerThreadFactory");
  }
}
