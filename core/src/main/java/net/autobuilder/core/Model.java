package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static net.autobuilder.core.Processor.rawType;

final class Model {

  private static final String SUFFIX = "_Builder";

  final TypeName sourceClass;
  final TypeName generatedClass;
  final TypeElement avType;
  final List<Parameter> parameters;

  private Model(TypeName sourceClass,
                TypeName generatedClass, TypeElement avType,
                ExecutableElement avConstructor) {
    this.sourceClass = sourceClass;
    this.generatedClass = generatedClass;
    this.avType = avType;
    this.parameters = Parameter.scan(avConstructor, avType);
  }

  static Model create(TypeElement sourceClassElement, TypeElement avType) {
    TypeName sourceClass = TypeName.get(sourceClassElement.asType());
    List<ExecutableElement> constructors = ElementFilter.constructorsIn(
        avType.getEnclosedElements());
    if (constructors.size() != 1) {
      throw new ValidationException(
          "Expecting exactly one constructor", avType);
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
            "@AutoBuilder and @AutoValue.Builder cannot be used together.",
            sourceClassElement, Diagnostic.Kind.WARNING);
      }
      throw new ValidationException(
          avType + " has a private constructor.",
          sourceClassElement, Diagnostic.Kind.ERROR);
    }
    return new Model(sourceClass, abPeer(sourceClass), avType,
        constructor);
  }

  private static TypeName abPeer(TypeName type) {
    String name = String.join("_", rawType(type).simpleNames()) + SUFFIX;
    ClassName className = rawType(type).topLevelClassName().peerClass(name);
    List<TypeName> typeargs = Processor.typeArguments(type);
    if (typeargs.isEmpty()) {
      return className;
    }
    return ParameterizedTypeName.get(className, typeargs.toArray(
        new TypeName[typeargs.size()]));
  }

  List<TypeVariableName> typevars() {
    return avType.getTypeParameters().stream()
        .map(TypeVariableName::get)
        .collect(toList());
  }
}
