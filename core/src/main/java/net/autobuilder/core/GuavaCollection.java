package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.Map;

import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static net.autobuilder.core.Util.typeArgumentSubtypes;
import static net.autobuilder.core.Util.typeArguments;

final class GuavaCollection extends Collectionish.Base {

  private static final ClassName MAP_ENTRY = ClassName.get(Map.Entry.class);
  private static final String GCC = "com.google.common.collect.";

  private final ClassName setterParameterClassName;

  private GuavaCollection(
      String className,
      Collectionish.CollectionType type,
      ClassName setterParameterClassName) {
    super(className, "java.lang.Iterable", type);
    this.setterParameterClassName = setterParameterClassName;
  }

  static Collectionish.Base ofGuava(
      String simpleName,
      Class<?> setterParameterClass,
      Collectionish.CollectionType type) {
    return new GuavaCollection(GCC + simpleName, type,
        ClassName.get(setterParameterClass));
  }

  @Override
  CodeBlock accumulatorInitBlock(FieldSpec builderField) {
    return CodeBlock.builder().addStatement("this.$N = $T.builder()",
        builderField, collectionClassName()).build();
  }

  @Override
  CodeBlock emptyBlock() {
    return CodeBlock.of("$T.of()", collectionClassName());
  }

  @Override
  ParameterizedTypeName accumulatorType(Parameter parameter) {
    return ParameterizedTypeName.get(collectionClassName().nestedClass("Builder"),
        typeArguments(parameter.variableElement.asType()));
  }

  @Override
  ParameterizedTypeName accumulatorOverloadArgumentType(Parameter parameter) {
    TypeName[] typeArguments = typeArgumentSubtypes(
        parameter.variableElement);
    return collectionType == Collectionish.CollectionType.LIST ?
        ParameterizedTypeName.get(overloadArgumentType(), typeArguments) :
        ParameterizedTypeName.get(overloadArgumentType(),
            subtypeOf(ParameterizedTypeName.get(MAP_ENTRY, typeArguments)));
  }

  @Override
  CodeBlock setterAssignment(Parameter parameter) {
    FieldSpec field = parameter.asField();
    ParameterSpec p = parameter.asSetterParameter();
    return CodeBlock.builder()
        .addStatement("this.$N = $N != null ? $T.copyOf($N) : null",
            field, p, collectionClassName(), p)
        .build();
  }

  @Override
  CodeBlock buildBlock(FieldSpec field) {
    return CodeBlock.of("$N.build()", field);
  }

  @Override
  ParameterSpec setterParameter(Parameter parameter) {
    TypeName type = ParameterizedTypeName.get(setterParameterClassName,
        typeArgumentSubtypes(parameter.variableElement));
    return ParameterSpec.builder(type, parameter.setterName).build();
  }
}
