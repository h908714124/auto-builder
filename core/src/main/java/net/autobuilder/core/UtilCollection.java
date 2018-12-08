package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.List;

import static net.autobuilder.core.Collectionish.CollectionType.LIST;
import static net.autobuilder.core.Model.withTypevars;
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
  DeclaredType accumulatorType(Parameter parameter) {
    TypeTool tool = TypeTool.get();
    List<? extends TypeMirror> typeArguments = tool.getDeclaredType(parameter.variableElement.asType()).getTypeArguments();
    return tool.getDeclaredType(accumulatorClass.packageName() + '.' + accumulatorClass.simpleName(), typeArguments);
  }

  @Override
  ParameterizedTypeName accumulatorOverloadArgumentType(Parameter parameter) {
    return ParameterizedTypeName.get(overloadArgumentType(),
        typeArgumentSubtypes(
            parameter.variableElement));
  }

  @Override
  CodeBlock setterAssignment(Parameter parameter) {
    FieldSpec field = parameter.asField();
    ParameterSpec p = parameter.asSetterParameter();
    return CodeBlock.builder()
        .addStatement("this.$N = $N", field, p)
        .build();
  }

  @Override
  CodeBlock buildBlock(FieldSpec field) {
    return CodeBlock.of("$N", field);
  }

  @Override
  ParameterSpec setterParameter(Parameter parameter) {
    TypeName type = withTypevars(
        collectionClassName(),
        typeArguments(parameter.variableElement));
    return ParameterSpec.builder(type, parameter.setterName).build();
  }
}
