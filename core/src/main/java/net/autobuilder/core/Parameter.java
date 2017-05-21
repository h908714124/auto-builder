package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.autobuilder.core.AutoBuilderProcessor.rawType;
import static net.autobuilder.core.Optionalish.OPTIONAL_CLASS;
import static net.autobuilder.core.Util.downcase;
import static net.autobuilder.core.Util.isDistinct;
import static net.autobuilder.core.Util.upcase;

final class Parameter {

  private static final Pattern GETTER_PATTERN =
      Pattern.compile("^get[A-Z].*$");
  private static final Pattern IS_PATTERN =
      Pattern.compile("^is[A-Z].*$");

  private final VariableElement variableElement;
  final String setterName;
  final String getterName;
  final TypeName type;

  private final Optionalish optionalish;
  private final Collectionish collectionish;

  private final Util util;

  private Parameter(
      Util util,
      VariableElement variableElement,
      String setterName,
      String getterName,
      TypeName type,
      Optionalish optionalish,
      Collectionish collectionish) {
    this.util = util;
    this.variableElement = variableElement;
    this.setterName = setterName;
    this.getterName = getterName;
    this.type = type;
    this.optionalish = optionalish;
    this.collectionish = collectionish;
  }

  static List<Parameter> scan(
      Util util,
      ExecutableElement constructor,
      TypeElement avType) {
    Set<String> methodNames = methodNames(avType);
    List<Parameter> parameters = new ArrayList<>();
    for (VariableElement variableElement : constructor.getParameters()) {
      String name = variableElement.getSimpleName().toString();
      TypeName type = TypeName.get(variableElement.asType());
      String getterName = matchingAccessor(methodNames, variableElement);
      String setterName = setterName(name, type);
      Optionalish optionalish = Optionalish.create(type);
      Collectionish collectionish = Collectionish.create(type);
      parameters.add(new Parameter(util, variableElement, setterName, getterName, type,
          optionalish, collectionish));
    }
    if (!parameters.stream()
        .map(p -> p.fieldNames().stream())
        .flatMap(Function.identity())
        .collect(isDistinct()) ||
        !parameters.stream()
            .map(p -> p.setterNames().stream())
            .flatMap(Function.identity())
            .collect(isDistinct())) {
      parameters = parameters.stream()
          .map(Parameter::noBuilder)
          .collect(toList());
    }
    if (!parameters.stream()
        .map(p -> p.setterNames().stream())
        .flatMap(Function.identity())
        .collect(isDistinct())) {
      parameters = parameters.stream()
          .map(Parameter::originalSetter)
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

  FieldSpec asBuilderField() {
    return FieldSpec.builder(builderType(),
        builderFieldName()).addModifiers(PRIVATE).build();
  }

  ParameterizedTypeName builderType() {
    ParameterizedTypeName typeName =
        (ParameterizedTypeName) TypeName.get(variableElement.asType());
    return ParameterizedTypeName.get(collectionish.className.nestedClass("Builder"),
        typeName.typeArguments.toArray(new TypeName[typeName.typeArguments.size()]));
  }

  ParameterSpec asParameter() {
    TypeName type = this.type;
    if (collectionish != null && collectionish.wildTyping) {
      ParameterizedTypeName typeName = (ParameterizedTypeName) TypeName.get(
          variableElement.asType());
      TypeName[] typeArguments =
          typeName.typeArguments.stream()
              .map(util::subtypeOf)
              .toArray(TypeName[]::new);
      type = ParameterizedTypeName.get(collectionish.setterParameterClassName,
          typeArguments);
    }
    return ParameterSpec.builder(type, setterName).build();
  }

  Optional<Optionalish> optionalish() {
    return Optional.ofNullable(optionalish);
  }

  Optional<Collectionish> collectionish() {
    return Optional.ofNullable(collectionish);
  }

  private List<String> setterNames() {
    if (collectionish == null || !collectionish.hasBuilder()) {
      return singletonList(setterName);
    }
    return asList(setterName, accumulatorName(collectionish));
  }

  String accumulatorName(Collectionish collectionish) {
    return collectionish.type.accumulatorPrefix + upcase(setterName);
  }

  private List<String> fieldNames() {
    if (collectionish == null || !collectionish.hasBuilder()) {
      return singletonList(setterName);
    }
    return asList(setterName, builderFieldName());
  }

  private String builderFieldName() {
    return downcase(setterName) + "Builder";
  }

  private Parameter originalSetter() {
    return new Parameter(util, variableElement, variableElement.getSimpleName().toString(),
        getterName, type,
        optionalish, collectionish);
  }

  private Parameter noBuilder() {
    if (collectionish == null || !collectionish.hasBuilder()) {
      return this;
    }
    return new Parameter(util, variableElement, setterName, getterName, type,
        optionalish, collectionish.noBuilder());
  }

  CodeBlock setterAssignment() {
    if (collectionish == null || collectionish.setterAssignment == null) {
      FieldSpec field = asField().build();
      ParameterSpec p = asParameter();
      return CodeBlock.builder()
          .addStatement("this.$N = $N", field, p).build();
    }
    return collectionish.setterAssignment.apply(this);
  }

  Optional<ParameterizedTypeName> addAllType() {
    if (collectionish == null || !collectionish.hasBuilder() || !collectionish.wildTyping) {
      return Optional.empty();
    }
    ParameterizedTypeName typeName = (ParameterizedTypeName) type;
    ClassName iterableClass = ClassName.get(Iterable.class);
    if (collectionish.type.typeParams == 1 &&
        typeName.rawType.equals(iterableClass)) {
      return Optional.empty();
    }
    TypeName[] typeArguments =
        typeName.typeArguments.stream()
            .map(util::subtypeOf)
            .toArray(TypeName[]::new);
    if (collectionish.type == Collectionish.CollectionType.LIST) {
      return Optional.of(ParameterizedTypeName.get(
          iterableClass, typeArguments));
    }
    return Optional.of(
        ParameterizedTypeName.get(iterableClass,
            WildcardTypeName.subtypeOf(ParameterizedTypeName.get(
                ClassName.get(Map.Entry.class), typeArguments))));
  }

}
