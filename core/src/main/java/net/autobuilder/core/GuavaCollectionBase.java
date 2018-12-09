package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static net.autobuilder.core.Util.typeArgumentSubtypes;

final class GuavaCollectionBase extends CollectionBase {

  private static final String GCC = "com.google.common.collect.";

  private final ClassName setterParameterClassName;

  private GuavaCollectionBase(
      String className,
      CollectionParameter.CollectionType type,
      ClassName setterParameterClassName) {
    super(className, "java.lang.Iterable", type);
    this.setterParameterClassName = setterParameterClassName;
  }

  static CollectionBase ofGuava(
      String simpleName,
      Class<?> setterParameterClass,
      CollectionParameter.CollectionType type) {
    return new GuavaCollectionBase(GCC + simpleName, type,
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
  DeclaredType accumulatorType(RegularParameter parameter) {
    TypeTool tool = TypeTool.get();
    List<? extends TypeMirror> typeArguments = tool.getDeclaredType(parameter.variableElement.asType()).getTypeArguments();
    return tool.getDeclaredType(collectionClassName + ".Builder", typeArguments);
  }

  @Override
  DeclaredType accumulatorOverloadArgumentType(RegularParameter parameter) {
    TypeMirror[] typeArguments = typeArgumentSubtypes(parameter.variableElement);
    TypeTool tool = TypeTool.get();
    return collectionType == CollectionParameter.CollectionType.LIST ?
        tool.getDeclaredType(overloadArgumentType().asType(), typeArguments) :
        tool.getDeclaredType(overloadArgumentType().asType(),
            tool.asExtendsWildcard(tool.getDeclaredType(tool.getTypeElement(Map.Entry.class).asType(), typeArguments)));
  }

  @Override
  CodeBlock setterAssignment(RegularParameter parameter) {
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
  ParameterSpec setterParameter(RegularParameter parameter) {
    TypeName type = ParameterizedTypeName.get(setterParameterClassName,
        Arrays.stream(typeArgumentSubtypes(parameter.variableElement))
            .map(TypeName::get)
            .toArray(TypeName[]::new));
    return ParameterSpec.builder(type, parameter.setterName).build();
  }
}
