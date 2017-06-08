package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.Collections;

import static net.autobuilder.core.Collectionish.CollectionType.LIST;
import static net.autobuilder.core.Model.withTypevars;
import static net.autobuilder.core.ParaParameter.AS_SETTER_PARAMETER;
import static net.autobuilder.core.Util.typeArgumentSubtypes;
import static net.autobuilder.core.Util.typeArguments;

final class UtilCollection extends Collectionish.Base {

  private final String emptyMethod;
  private final ClassName accumulatorClass;

  private UtilCollection(
      ClassName accumulatorClass,
      String emptyMethod,
      String className,
      Collectionish.CollectionType type,
      String accumulatorAddAllType) {
    super(className, accumulatorAddAllType, type);
    this.accumulatorClass = accumulatorClass;
    this.emptyMethod = emptyMethod;
  }

  static Collectionish.Base ofUtil(
      String simpleName,
      String emptyMethod,
      Class<?> builderClass,
      Collectionish.CollectionType collectionType) {
    String accumulatorAddAllType = collectionType == LIST ?
        "java.util.Collection" :
        "java.util.Map";
    return new UtilCollection(
        ClassName.get(builderClass),
        emptyMethod,
        "java.util." + simpleName,
        collectionType,
        accumulatorAddAllType);
  }

  @Override
  CodeBlock accumulatorInitBlock(FieldSpec builderField) {
    return CodeBlock.builder().addStatement("this.$N = new $T<>()",
        builderField, accumulatorClass).build();
  }

  @Override
  CodeBlock emptyBlock() {
    return CodeBlock.of("$T.$L()", Collections.class, emptyMethod);
  }

  @Override
  ParameterizedTypeName accumulatorType(Parameter parameter) {
    ParameterizedTypeName typeName =
        (ParameterizedTypeName) parameter.type;
    return ParameterizedTypeName.get(accumulatorClass,
        typeName.typeArguments.toArray(new TypeName[0]));
  }

  @Override
  ParameterizedTypeName accumulatorOverloadArgumentType(Parameter parameter) {
    TypeName[] typeArguments = typeArgumentSubtypes(
        parameter.variableElement);
    return ParameterizedTypeName.get(overloadArgumentType(),
        typeArguments);
  }

  @Override
  CodeBlock setterAssignment(Parameter parameter) {
    FieldSpec field = parameter.asField();
    ParameterSpec p = AS_SETTER_PARAMETER.apply(parameter);
    return CodeBlock.builder()
        .addStatement("this.$N = $N", field, p)
        .build();
  }

  @Override
  CodeBlock buildBlock(ParameterSpec builder, FieldSpec field) {
    return CodeBlock.of("$N.$N", builder, field);
  }

  @Override
  ParameterSpec setterParameter(Parameter parameter) {
    TypeName type = withTypevars(
        collectionClassName(),
        typeArguments(parameter.variableElement));
    return ParameterSpec.builder(type, parameter.setterName).build();
  }
}
