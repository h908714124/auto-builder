package net.autobuilder.core;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.autobuilder.core.AutoBuilderProcessor.rawType;
import static net.autobuilder.core.Optionalish.OPTIONAL_CLASS;

final class Parameter {

  private static final Pattern GETTER_PATTERN =
      Pattern.compile("^get[A-Z].*$");
  private static final Pattern IS_PATTERN =
      Pattern.compile("^is[A-Z].*$");

  private final String name;
  final String setterName;
  final String getterName;
  final TypeName type;

  private final Optionalish optionalish;
  private final Collectionish collectionish;

  private Parameter(String name,
                    String setterName,
                    String getterName,
                    TypeName type) {
    this.name = name;
    this.setterName = setterName;
    this.getterName = getterName;
    this.type = type;
    this.optionalish = Optionalish.create(type);
    this.collectionish = Collectionish.create(type);
  }

  static List<Parameter> scan(ExecutableElement constructor,
                              TypeElement avType) {
    Set<String> methodNames = methodNames(avType);
    List<Parameter> parameters = new ArrayList<>();
    for (VariableElement variableElement : constructor.getParameters()) {
      String name = variableElement.getSimpleName().toString();
      TypeName type = TypeName.get(variableElement.asType());
      String accessorName = matchingAccessor(methodNames, variableElement);
      String setterName = setterName(name, type);
      parameters.add(new Parameter(name, setterName, accessorName, type));
    }
    Set<String> duplicateCheck = parameters.stream()
        .map(p -> p.setterName)
        .collect(toSet());
    if (duplicateCheck.size() < parameters.size()) {
      return parameters.stream()
          .map(p -> new Parameter(p.name, p.name, p.getterName, p.type))
          .collect(toList());
    }
    return parameters;
  }

  static String setterName(String name, TypeName type) {
    if (GETTER_PATTERN.matcher(name).matches()) {
      return Character.toLowerCase(name.charAt(3)) + name.substring(4);
    }
    if (type.equals(TypeName.BOOLEAN) &&
        IS_PATTERN.matcher(name).matches()) {
      return Character.toLowerCase(name.charAt(2)) + name.substring(3);
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
      String getter = "is" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
      if (methodNames.contains(getter)) {
        return getter;
      }
    }
    String getter = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    if (!methodNames.contains(getter)) {
      throw new ValidationException("no matching accessor: " + name, constructorArgument);
    }
    return getter;
  }

  FieldSpec.Builder asField() {
    return FieldSpec.builder(type, setterName).addModifiers(PRIVATE);
  }

  FieldSpec asInitializedField() {
    FieldSpec.Builder fieldBuilder = asField();
    if (optionalish != null) {
      fieldBuilder.initializer("$T.empty()", optionalish.wrapper);
    } else if (rawType(type).equals(OPTIONAL_CLASS)) {
      fieldBuilder.initializer("$T.empty()", OPTIONAL_CLASS);
    } else if (collectionish != null) {
      fieldBuilder.initializer("$T.$L()",
          collectionish.factoryClassName, collectionish.emptyMethod);
    }
    return fieldBuilder.build();
  }

  ParameterSpec asParameter() {
    return ParameterSpec.builder(type, setterName).build();
  }

  Optional<Optionalish> optionalish() {
    return Optional.ofNullable(optionalish);
  }

  Optional<Collectionish> collectionish() {
    return Optional.ofNullable(collectionish);
  }
}
