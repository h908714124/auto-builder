package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static javax.tools.Diagnostic.Kind.WARNING;
import static net.autobuilder.core.Processor.rawType;
import static net.autobuilder.core.Processor.typeArguments;

final class Model {

  private static final String SUFFIX = "_Builder";

  final TypeName sourceClass;
  final TypeName generatedClass;
  final TypeName simpleBuilderClass;
  final Optional<ClassName> optionalRefTrackingBuilderClass;
  final TypeElement avType;
  final List<Parameter> parameters;

  private Model(TypeName sourceClass,
                TypeName generatedClass, TypeElement avType,
                ExecutableElement avConstructor,
                TypeName simpleBuilderClass,
                Optional<ClassName> optionalRefTrackingBuilderClass) {
    this.sourceClass = sourceClass;
    this.generatedClass = generatedClass;
    this.avType = avType;
    this.simpleBuilderClass = simpleBuilderClass;
    this.optionalRefTrackingBuilderClass = optionalRefTrackingBuilderClass;
    this.parameters = Parameter.scan(avConstructor, avType);
  }

  static Model create(TypeElement sourceClassElement, TypeElement avType) {
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
            sourceClassElement, WARNING);
      }
      throw new ValidationException(
          avType + " has a private constructor.",
          sourceClassElement);
    }
    TypeName generatedClass = abPeer(sourceClass);
    TypeName simpleBuilderClass = nestedClass(generatedClass, "SimpleBuilder");
    Optional<ClassName> optionalRefTrackingBuilderClass = Optional.empty();
    if (typeArguments(generatedClass).isEmpty()) {
      optionalRefTrackingBuilderClass =
          Optional.of(rawType(generatedClass).nestedClass("RefTrackingBuilder"));
    }
    return new Model(sourceClass, generatedClass, avType,
        constructor, simpleBuilderClass, optionalRefTrackingBuilderClass);
  }

  private static TypeName abPeer(TypeName type) {
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
}
