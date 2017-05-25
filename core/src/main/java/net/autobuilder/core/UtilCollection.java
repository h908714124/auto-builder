package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static net.autobuilder.core.Collectionish.CollectionType.LIST;
import static net.autobuilder.core.Collectionish.normalAddAllType;
import static net.autobuilder.core.Util.typeArgumentSubtypes;

final class UtilCollection extends Collectionish.Base {

  private final String emptyMethod;
  private final ClassName accumulatorClass;

  private UtilCollection(
      ClassName accumulatorClass,
      String emptyMethod,
      ClassName className,
      Function<FieldSpec, CodeBlock> builderInitBlock,
      Collectionish.CollectionType type,
      ClassName setterParameterClassName,
      boolean wildTyping) {
    super(className, builderInitBlock, type, setterParameterClassName,
        wildTyping);
    this.accumulatorClass = accumulatorClass;
    this.emptyMethod = emptyMethod;
  }

  static Collectionish.Base ofUtil(
      Class<?> className, String emptyMethod, Class<?> builderClass, Collectionish.CollectionType type) {
    return new UtilCollection(
        ClassName.get(builderClass),
        emptyMethod,
        ClassName.get(className),
        builderField ->
            CodeBlock.builder().addStatement("this.$N = new $T<>()",
                builderField, builderClass).build(),
        type,
        ClassName.get(className),
        false);
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
  CodeBlock setterAssignment(Parameter parameter) {
    FieldSpec field = parameter.asField();
    ParameterSpec p = parameter.model.asParameter.apply(parameter);
    return CodeBlock.builder()
        .addStatement("this.$N = $N", field, p)
        .build();
  }

  @Override
  CodeBlock buildBlock(ParameterSpec builder, FieldSpec field) {
    return CodeBlock.of("$N.$N", builder, field);
  }

  @Override
  Optional<ParameterizedTypeName> addAllType(Parameter parameter) {
    ClassName accumulatorAddAllType = collectionType == LIST ?
        ClassName.get(Collection.class) :
        ClassName.get(Map.class);
    return collectionType == LIST ?
        normalAddAllType(parameter, collectionType, accumulatorAddAllType) :
        Optional.of(
            ParameterizedTypeName.get(accumulatorAddAllType,
                typeArgumentSubtypes(parameter.variableElement)));

  }
}
