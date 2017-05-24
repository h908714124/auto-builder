package net.autobuilder.core;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.autobuilder.core.AutoBuilderProcessor.rawType;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

final class Model {

  private static final String SUFFIX = "_Builder";
  private static final Modifier[] PUBLIC_MODIFIER = {PUBLIC};
  private static final Modifier[] NO_MODIFIERS = new Modifier[0];

  private final TypeElement sourceClassElement;

  private final ClassName optionalRefTrackingBuilderClass;

  final TypeName generatedClass;
  final TypeName simpleBuilderClass;
  final TypeElement avType;
  final List<ParaParameter> parameters;
  final TypeName sourceClass;
  final Util util;

  private Model(Util util, TypeElement sourceClassElement,
                TypeName generatedClass,
                TypeElement avType,
                List<ParaParameter> parameters,
                TypeName simpleBuilderClass,
                ClassName optionalRefTrackingBuilderClass) {
    this.util = util;
    this.sourceClassElement = sourceClassElement;
    this.generatedClass = generatedClass;
    this.avType = avType;
    this.simpleBuilderClass = simpleBuilderClass;
    this.optionalRefTrackingBuilderClass = optionalRefTrackingBuilderClass;
    this.parameters = parameters;
    this.sourceClass = TypeName.get(sourceClassElement.asType());
  }

  static Model create(
      Util util,
      TypeElement sourceClassElement, TypeElement avType) {
    TypeName sourceClass = TypeName.get(sourceClassElement.asType());
    List<ExecutableElement> constructors = ElementFilter.constructorsIn(
        avType.getEnclosedElements());
    if (constructors.size() != 1) {
      throw new ValidationException(
          avType + " does not have exactly one constructor.", sourceClassElement);
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
          avType + " has a private constructor.",
          sourceClassElement);
    }
    TypeName generatedClass = abPeer(sourceClass);
    TypeName simpleBuilderClass = nestedClass(generatedClass, "SimpleBuilder");
    ClassName optionalRefTrackingBuilderClass =
        typeArguments(generatedClass).isEmpty() ?
            rawType(generatedClass).nestedClass("RefTrackingBuilder") :
            null;
    return new Model(util, sourceClassElement, generatedClass, avType,
        Parameter.scan(util, constructor, avType), simpleBuilderClass,
        optionalRefTrackingBuilderClass);
  }

  static TypeName abPeer(TypeName type) {
    String name = String.join("_", rawType(type).simpleNames()) + SUFFIX;
    ClassName className = rawType(type).topLevelClassName().peerClass(name);
    return withTypevars(className, typeArguments(type));
  }

  private static TypeName nestedClass(TypeName generatedClass, String name) {
    return withTypevars(rawType(generatedClass).nestedClass(name),
        typeArguments(generatedClass));
  }

  private static TypeName withTypevars(ClassName className, List<TypeName> typevars) {
    if (typevars.isEmpty()) {
      return className;
    }
    return ParameterizedTypeName.get(className, typevars.toArray(
        new TypeName[typevars.size()]));
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
      return PUBLIC_MODIFIER;
    }
    return NO_MODIFIERS;
  }

  Optional<ClassName> optionalRefTrackingBuilderClass() {
    return Optional.ofNullable(optionalRefTrackingBuilderClass);
  }

  String cacheWarning() {
    return "Caching not implemented: " +
        rawType(sourceClass).simpleName() +
        "<" +
        typevars().stream()
            .map(TypeVariableName::toString)
            .collect(joining(", ")) +
        "> has type parameters";
  }

  private static List<TypeName> typeArguments(TypeName typeName) {
    if (typeName instanceof ParameterizedTypeName) {
      return ((ParameterizedTypeName) typeName).typeArguments;
    }
    return Collections.emptyList();
  }

}
