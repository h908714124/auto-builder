package net.autobuilder.core;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.autobuilder.core.Util.downcase;
import static net.autobuilder.core.Util.isDistinct;
import static net.autobuilder.core.Util.upcase;

final class Parameter extends ParaParameter {

  private static final Pattern GETTER_PATTERN =
      Pattern.compile("^get[A-Z].*$");
  private static final Pattern IS_PATTERN =
      Pattern.compile("^is[A-Z].*$");

  final VariableElement variableElement;
  final String setterName;
  final String getterName;
  final TypeName type;

  final Model model;

  private final Util util;

  private Parameter(
      Util util,
      VariableElement variableElement,
      String setterName,
      String getterName,
      TypeName type,
      Model model) {
    this.util = util;
    this.variableElement = variableElement;
    this.setterName = setterName;
    this.getterName = getterName;
    this.type = type;
    this.model = model;
  }

  static List<ParaParameter> scan(
      Model model,
      Util util,
      ExecutableElement constructor,
      TypeElement avType) {
    Set<String> methodNames = methodNames(avType);
    List<ParaParameter> parameters = constructor.getParameters().stream()
        .map(variableElement -> {
          String name = variableElement.getSimpleName().toString();
          TypeName type = TypeName.get(variableElement.asType());
          String getterName = matchingAccessor(methodNames, variableElement);
          String setterName = setterName(name, type);
          Parameter parameter = new Parameter(
              util, variableElement, setterName, getterName, type, model);
          return Collectionish.create(parameter)
              .orElse(Optionalish.create(parameter).orElse(parameter));
        })
        .collect(toList());
    if (!parameters.stream()
        .map(ParaParameter.FIELD_NAMES)
        .map(List::stream)
        .flatMap(Function.identity())
        .collect(isDistinct()) ||
        !parameters.stream()
            .map(ParaParameter.METHOD_NAMES)
            .map(List::stream)
            .flatMap(Function.identity())
            .collect(isDistinct())) {
      parameters = parameters.stream()
          .map(ParaParameter.NO_ACCUMULATOR)
          .collect(toList());
    }
    if (!parameters.stream()
        .map(ParaParameter.METHOD_NAMES)
        .map(List::stream)
        .flatMap(Function.identity())
        .collect(isDistinct())) {
      parameters = parameters.stream()
          .map(ParaParameter.ORIGINAL_SETTER)
          .collect(toList());
    }
    return parameters;
  }

  static String setterName(String name, TypeName type) {
    if (GETTER_PATTERN.matcher(name).matches()) {
      return downcase(name.substring(3));
    }
    if (type.equals(TypeName.BOOLEAN) &&
        IS_PATTERN.matcher(name).matches()) {
      return downcase(name.substring(2));
    }
    return name;
  }

  private static Set<String> methodNames(TypeElement avType) {
    return ElementFilter.methodsIn(
        avType.getEnclosedElements()).stream()
        .filter(m -> m.getParameters().isEmpty())
        .filter(m -> !m.getReturnType().equals(TypeName.VOID))
        .map(ExecutableElement::getSimpleName)
        .map(CharSequence::toString)
        .collect(Collectors.toSet());
  }

  private static String matchingAccessor(Set<String> methodNames,
                                         VariableElement constructorArgument) {
    String name = constructorArgument.getSimpleName().toString();
    TypeName type = TypeName.get(constructorArgument.asType());
    if (methodNames.contains(name)) {
      return name;
    }
    if (type.equals(TypeName.BOOLEAN)) {
      String getter = "is" + upcase(name);
      if (methodNames.contains(getter)) {
        return getter;
      }
    }
    String getter = "get" + upcase(name);
    if (!methodNames.contains(getter)) {
      throw new ValidationException("no matching accessor: " + name, constructorArgument);
    }
    return getter;
  }

  FieldSpec asField() {
    return FieldSpec.builder(type, setterName).addModifiers(PRIVATE).build();
  }

  Parameter originalSetter() {
    return new Parameter(util, variableElement, variableElement.getSimpleName().toString(),
        getterName, type, model);
  }

  @Override
  <R, P> R accept(Cases<R, P> cases, P p) {
    return cases.parameter(this, p);
  }
}
