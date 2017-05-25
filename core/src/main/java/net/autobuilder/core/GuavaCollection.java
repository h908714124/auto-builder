package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.Optional;
import java.util.function.Function;

import static net.autobuilder.core.Collectionish.normalAddAllType;

final class GuavaCollection extends Collectionish.Base {

  private static final String GCC = "com.google.common.collect";

  private GuavaCollection(
      ClassName className,
      Function<FieldSpec, CodeBlock> builderInitBlock,
      Collectionish.CollectionType type,
      ClassName setterParameterClassName,
      boolean wildTyping) {
    super(className, builderInitBlock, type, setterParameterClassName,
        wildTyping);
  }

  static Collectionish.Base ofGuava(
      String simpleName,
      Class<?> setterParameterClass,
      Collectionish.CollectionType type) {
    ClassName className = ClassName.get(GCC, simpleName);
    return new GuavaCollection(
        className,
        builderField ->
            CodeBlock.builder().addStatement("this.$N = $T.builder()",
                builderField, className).build(),
        type,
        ClassName.get(setterParameterClass),
        true);
  }

  @Override
  CodeBlock emptyBlock() {
    return CodeBlock.of("$T.of()", className);
  }

  @Override
  ParameterizedTypeName accumulatorType(Parameter parameter) {
    ParameterizedTypeName typeName =
        (ParameterizedTypeName) TypeName.get(parameter.variableElement.asType());
    return ParameterizedTypeName.get(className.nestedClass("Builder"),
        typeName.typeArguments.toArray(new TypeName[typeName.typeArguments.size()]));
  }

  @Override
  CodeBlock setterAssignment(Parameter parameter) {
    FieldSpec field = parameter.asField();
    ParameterSpec p = parameter.model.asParameter.apply(parameter);
    return CodeBlock.builder()
        .addStatement("this.$N = $N != null ? $T.copyOf($N) : null",
            field, p, className, p)
        .build();
  }

  @Override
  CodeBlock buildBlock(ParameterSpec builder, FieldSpec field) {
    return CodeBlock.of("$N.$N.build()", builder, field);
  }

  @Override
  Optional<ParameterizedTypeName> addAllType(Parameter parameter) {
    return normalAddAllType(parameter, collectionType, ClassName.get(Iterable.class));
  }
}
