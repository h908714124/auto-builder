package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.Optional;

import static net.autobuilder.core.Collectionish.normalAddAllType;
import static net.autobuilder.core.ParaParameter.AS_SETTER_PARAMETER;

final class GuavaCollection extends Collectionish.Base {

  private static final String GCC = "com.google.common.collect";

  private GuavaCollection(
      ClassName className,
      Collectionish.CollectionType type,
      ClassName setterParameterClassName,
      boolean wildTyping) {
    super(className, type, setterParameterClassName, wildTyping);
  }

  static Collectionish.Base ofGuava(
      String simpleName,
      Class<?> setterParameterClass,
      Collectionish.CollectionType type) {
    ClassName className = ClassName.get(GCC, simpleName);
    return new GuavaCollection(className, type,
        ClassName.get(setterParameterClass),
        true);
  }

  @Override
  CodeBlock accumulatorInitBlock(FieldSpec builderField) {
    return CodeBlock.builder().addStatement("this.$N = $T.builder()",
        builderField, collectionClassName).build();
  }

  @Override
  CodeBlock emptyBlock() {
    return CodeBlock.of("$T.of()", collectionClassName);
  }

  @Override
  ParameterizedTypeName accumulatorType(Parameter parameter) {
    ParameterizedTypeName typeName =
        (ParameterizedTypeName) TypeName.get(parameter.variableElement.asType());
    return ParameterizedTypeName.get(collectionClassName.nestedClass("Builder"),
        typeName.typeArguments.toArray(new TypeName[typeName.typeArguments.size()]));
  }

  @Override
  CodeBlock setterAssignment(Parameter parameter) {
    FieldSpec field = parameter.asField();
    ParameterSpec p = AS_SETTER_PARAMETER.apply(parameter);
    return CodeBlock.builder()
        .addStatement("this.$N = $N != null ? $T.copyOf($N) : null",
            field, p, collectionClassName, p)
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
