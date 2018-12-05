package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import net.autobuilder.AutoBuilder;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.autobuilder.core.AutoBuilderProcessor.rawType;
import static net.autobuilder.core.Util.typeArguments;

public final class Model {

  private static final String SUFFIX = "_Builder";
  private static final String REF_TRACKING_BUILDER = "RefTrackingBuilder";
  private static final String SIMPLE_BUILDER = "SimpleBuilder";

  private final TypeElement sourceClassElement;

  private final ExecutableElement constructor;

  private final ParameterSpec builderParameter;

  final Optional<ClassName> optionalRefTrackingBuilderClass;

  final TypeName generatedClass;
  final TypeName simpleBuilderClass;
  final TypeElement avType;
  private final TypeElement sourceClass;
  final Util util;


  private Model(Util util, TypeElement sourceClassElement,
                TypeName generatedClass,
                TypeElement avType,
                TypeName simpleBuilderClass,
                Optional<ClassName> optionalRefTrackingBuilderClass,
                ExecutableElement constructor) {
    this.util = util;
    this.sourceClassElement = sourceClassElement;
    this.generatedClass = generatedClass;
    this.avType = avType;
    this.simpleBuilderClass = simpleBuilderClass;
    this.optionalRefTrackingBuilderClass = optionalRefTrackingBuilderClass;
    this.sourceClass = sourceClassElement;
    this.constructor = constructor;
    this.builderParameter = ParameterSpec.builder(generatedClass, "builder").build();
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
    TypeName simpleBuilderClass = simpleBuilderClass(sourceClassElement, generatedClass);
    if (!sourceClassElement.getTypeParameters().isEmpty()) {
      throw new ValidationException("The class may not have type parameters.",
          sourceClassElement);
    }
    Optional<ClassName> optionalRefTrackingBuilderClass =
        sourceClassElement.getAnnotation(AutoBuilder.class).reuseBuilder() ?
            Optional.of(rawType(generatedClass).nestedClass(REF_TRACKING_BUILDER)) :
            Optional.empty();
    return new Model(util, sourceClassElement, generatedClass, avType,
        simpleBuilderClass,
        optionalRefTrackingBuilderClass, constructor);
  }

  List<ParaParameter> scan() {
    return Parameter.scan(this, constructor, avType);
  }

  private static TypeName generatedClass(TypeElement typeElement) {
    TypeName type = TypeName.get(typeElement.asType());
    String name = String.join("_", rawType(type).simpleNames()) + SUFFIX;
    ClassName className = rawType(type).topLevelClassName().peerClass(name);
    return withTypevars(className, typeArguments(typeElement));
  }

  private static TypeName simpleBuilderClass(
      TypeElement typeElement,
      TypeName generatedClass) {
    return withTypevars(
        rawType(generatedClass)
            .nestedClass(SIMPLE_BUILDER),
        typeArguments(typeElement));
  }

  static TypeName withTypevars(ClassName className, TypeName[] typevars) {
    if (typevars.length == 0) {
      return className;
    }
    return ParameterizedTypeName.get(className, typevars);
  }

  List<TypeVariableName> typevars() {
    return avType.getTypeParameters().stream()
        .map(TypeVariableName::get)
        .collect(toList());
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

  public ParameterSpec builderParameter() {
    return builderParameter;
  }

  TypeElement sourceClass() {
    return sourceClass;
  }
}
