package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.List;

import static net.autobuilder.core.CollectionParameter.CollectionType.LIST;
import static net.autobuilder.core.Model.withTypevars;
import static net.autobuilder.core.Util.typeArgumentSubtypes;
import static net.autobuilder.core.Util.typeArguments;

final class UtilCollectionBase extends CollectionBase {

  private final String emptyMethod;
  private final ClassName accumulatorClass;

  private UtilCollectionBase(
      ClassName accumulatorClass,
      String emptyMethod,
      String className,
      CollectionParameter.CollectionType type,
      String accumulatorAddAllType) {
    super(className, accumulatorAddAllType, type);
    this.accumulatorClass = accumulatorClass;
    this.emptyMethod = emptyMethod;
  }

  static CollectionBase ofUtil(
      String simpleName,
      String emptyMethod,
      Class<?> builderClass,
      CollectionParameter.CollectionType collectionType) {
    String accumulatorAddAllType = collectionType == LIST ?
        "java.util.Collection" :
        "java.util.Map";
    return new UtilCollectionBase(
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
  DeclaredType accumulatorType(RegularParameter parameter) {
    TypeTool tool = TypeTool.get();
    List<? extends TypeMirror> typeArguments = tool.getDeclaredType(parameter.variableElement.asType()).getTypeArguments();
    return tool.getDeclaredType(accumulatorClass.packageName() + '.' + accumulatorClass.simpleName(), typeArguments);
  }

  @Override
  DeclaredType accumulatorOverloadArgumentType(RegularParameter parameter) {
    TypeTool tool = TypeTool.get();
    return tool.getDeclaredType(overloadArgumentType().asType(),
        typeArgumentSubtypes(parameter.variableElement));
  }

  @Override
  CodeBlock setterAssignment(RegularParameter parameter) {
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
  ParameterSpec setterParameter(RegularParameter parameter) {
    TypeName type = withTypevars(
        collectionClassName(),
        typeArguments(parameter.variableElement));
    return ParameterSpec.builder(type, parameter.setterName).build();
  }
}
