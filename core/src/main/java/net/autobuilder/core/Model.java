package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.List;

import static java.util.stream.Collectors.toList;

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

  static Model create(TypeName sourceClass, TypeElement avType) {
    List<ExecutableElement> constructors = ElementFilter.constructorsIn(
        avType.getEnclosedElements());
    if (constructors.size() != 1) {
      throw new ValidationException(
          "Expecting exactly one constructor", avType);
    }
    ExecutableElement constructor = constructors.get(0);
    return new Model(sourceClass, abPeer(sourceClass), avType,
        constructor);
  }

  private static TypeName abPeer(TypeName type) {
    String name = String.join("_", Processor.rawType(type).simpleNames()) + SUFFIX;
    ClassName className = Processor.rawType(type).topLevelClassName().peerClass(name);
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
