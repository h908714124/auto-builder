package net.autobuilder.core;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.autobuilder.core.Cleanse.detox;
import static net.autobuilder.core.Util.downcase;
import static net.autobuilder.core.Util.upcase;

public final class Parameter extends ParaParameter {

  private static final Pattern GETTER_PATTERN =
      Pattern.compile("^get[A-Z].*$");
  private static final Pattern IS_PATTERN =
      Pattern.compile("^is[A-Z].*$");

  // One parameter of the generated auto-value constructor
  public final VariableElement variableElement;

  public final TypeMirror type;

  public final String setterName;

  final String getterName;

  public final Model model;

  private Parameter(
      VariableElement variableElement,
      String setterName,
      String getterName,
      TypeMirror type,
      Model model) {
    this.variableElement = variableElement;
    this.setterName = setterName;
    this.getterName = getterName;
    this.type = type;
    this.model = model;
  }

  static List<ParaParameter> scan(
      Model model,
      ExecutableElement avConstructor,
      TypeElement avType) {
    Set<String> methodNames = methodNames(avType);
    List<ParaParameter> avConstructorParameters = avConstructor.getParameters().stream()
        .map(variableElement -> {
          String name = variableElement.getSimpleName().toString();
          TypeMirror type = variableElement.asType();
          String getterName = matchingAccessor(methodNames, variableElement);
          String setterName = setterName(name, type);
          Parameter parameter = new Parameter(
              variableElement, setterName, getterName, type, model);
          return Collectionish.maybeCreate(parameter)
              .orElse(Optionalish.maybeCreate(parameter)
                  .orElse(parameter));
        })
        .collect(toList());
    return detox(avConstructorParameters);
  }

  static String setterName(String name, TypeMirror type) {
    if (GETTER_PATTERN.matcher(name).matches()) {
      return downcase(name.substring(3));
    }
    if (type.getKind() == TypeKind.BOOLEAN &&
        IS_PATTERN.matcher(name).matches()) {
      return downcase(name.substring(2));
    }
    return name;
  }

  private static Set<String> methodNames(TypeElement avType) {
    return ElementFilter.methodsIn(
        avType.getEnclosedElements()).stream()
        .filter(m -> m.getParameters().isEmpty())
        .filter(m -> m.getReturnType().getKind() != TypeKind.VOID)
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

  public FieldSpec asField() {
    return FieldSpec.builder(TypeName.get(type), setterName).addModifiers(PRIVATE).build();
  }

  Parameter originalSetter() {
    return new Parameter(variableElement, variableElement.getSimpleName().toString(),
        getterName, type, model);
  }

  @Override
  <R, P> R accept(ParamCases<R, P> cases, P p) {
    return cases.parameter(this, p);
  }
}
