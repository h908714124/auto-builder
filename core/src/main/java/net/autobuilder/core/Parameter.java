package net.autobuilder.core;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.autobuilder.core.Util.downcase;
import static net.autobuilder.core.Util.isDistinct;
import static net.autobuilder.core.Util.upcase;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;

final class Parameter extends ParaParameter {

  private static final Pattern GETTER_PATTERN =
      Pattern.compile("^get[A-Z].*$");
  private static final Pattern IS_PATTERN =
      Pattern.compile("^is[A-Z].*$");

  final VariableElement variableElement;
  final String setterName;
  final String getterName;
  final TypeName type;

  private final Util util;

  private Parameter(
      Util util,
      VariableElement variableElement,
      String setterName,
      String getterName,
      TypeName type) {
    this.util = util;
    this.variableElement = variableElement;
    this.setterName = setterName;
    this.getterName = getterName;
    this.type = type;
  }

  static List<ParaParameter> scan(
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
          Parameter parameter = new Parameter(util, variableElement, setterName, getterName, type);
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

  FieldSpec asBuilderField() {
    return FieldSpec.builder(builderType(),
        builderFieldName()).addModifiers(PRIVATE).build();
  }

  ParameterizedTypeName builderType() {
    return collectionish.accumulatorType.apply(this);
  }

  ParameterSpec asParameter() {
    TypeName type = this.type;
    if (collectionish != null && collectionish.wildTyping) {
      TypeName[] typeArguments = Util.typeArgumentSubtypes(variableElement);
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
    if (collectionish == null || !collectionish.hasAccumulator()) {
      return singletonList(setterName);
    }
    return asList(setterName, accumulatorName(collectionish));
  }

  private List<String> fieldNames() {
    if (collectionish == null || !collectionish.hasAccumulator()) {
      return singletonList(setterName);
    }
    return asList(setterName, builderFieldName());
  }

  private String builderFieldName() {
    return downcase(setterName) + "Builder";
  }

  Parameter originalSetter() {
    return new Parameter(util, variableElement, variableElement.getSimpleName().toString(),
        getterName, type);
  }

  private Parameter noBuilder() {
    if (collectionish == null || !collectionish.hasAccumulator()) {
      return this;
    }
    return new Parameter(util, variableElement, setterName, getterName, type,
        optionalish, collectionish.noBuilder());
  }

  CodeBlock setterAssignment() {
    if (collectionish == null || collectionish.setterAssignment == null) {
      FieldSpec field = asField();
      ParameterSpec p = asParameter();
      return CodeBlock.builder()
          .addStatement("this.$N = $N", field, p).build();
    }
    return collectionish.setterAssignment.apply(this);
  }

  Optional<ParameterizedTypeName> addAllType() {
    if (collectionish == null ||
        !collectionish.hasAccumulator()) {
      return Optional.empty();
    }
    return collectionish.addAllType.apply(this);
  }

  CodeBlock addAllBlock(CodeBlock what) {
    return collectionish.addAllBlock.apply(this, what);
  }

  CodeBlock getFieldValueBlock(ParameterSpec builder) {
    FieldSpec field = asField();
    if (collectionish != null) {
      CodeBlock getCollection = CodeBlock.builder()
          .add("$N.$N != null ? $N.$N : ",
              builder, field, builder, field)
          .add(collectionish.emptyBlock.get())
          .build();
      if (collectionish.hasAccumulator()) {
        FieldSpec builderField = asBuilderField();
        return CodeBlock.builder().add("$N.$N != null ? ", builder, builderField)
            .add(collectionish.buildBlock.apply(builder, builderField))
            .add(" :\n        ")
            .add(getCollection)
            .build();
      } else {
        return getCollection;
      }
    }
    if (optionalish != null) {
      return CodeBlock.of("$N.$N != null ? $N.$N : $T.empty()",
          builder, field,
          builder, field,
          optionalish.wrapper);
    }
    return CodeBlock.of("$N.$N", builder, field);
  }

  @Override
  <R> R accept(Cases<R> cases) {
    return cases.parameter(this);
  }
}
