package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static javax.lang.model.element.Modifier.FINAL;
import static net.autobuilder.core.AutoBuilderProcessor.rawType;

final class Optionalish extends ParaParameter {

  private static final ClassName OPTIONAL_CLASS =
      ClassName.get(Optional.class);

  private static final Map<ClassName, TypeName> OPTIONAL_PRIMITIVES;

  static {
    OPTIONAL_PRIMITIVES = new HashMap<>(3);
    OPTIONAL_PRIMITIVES.put(ClassName.get(OptionalInt.class), TypeName.INT);
    OPTIONAL_PRIMITIVES.put(ClassName.get(OptionalDouble.class), TypeName.DOUBLE);
    OPTIONAL_PRIMITIVES.put(ClassName.get(OptionalLong.class), TypeName.LONG);
  }

  private static final String OF_NULLABLE = "ofNullable";

  final Parameter parameter;

  private final ClassName wrapper;
  private final TypeName wrapped;

  private final String of;

  private Optionalish(
      Parameter parameter,
      ClassName wrapper,
      TypeName wrapped,
      String of) {
    this.parameter = parameter;
    this.wrapper = wrapper;
    this.wrapped = wrapped;
    this.of = of;
  }

  static Optional<ParaParameter> create(Parameter parameter) {
    if (parameter.type instanceof ClassName) {
      ClassName className = rawType(parameter.type);
      TypeName primitive = OPTIONAL_PRIMITIVES.get(className);
      return primitive != null ?
          Optional.of(new Optionalish(parameter, className, primitive, "of")) :
          Optional.empty();
    }
    if (!(parameter.type instanceof ParameterizedTypeName)) {
      return Optional.empty();
    }
    ParameterizedTypeName type = (ParameterizedTypeName) parameter.type;
    if (!type.rawType.equals(OPTIONAL_CLASS)) {
      return Optional.empty();
    }
    if (type.typeArguments.size() != 1) {
      return Optional.empty();
    }
    TypeName wrapped = type.typeArguments.get(0);
    if (rawType(wrapped).equals(OPTIONAL_CLASS)) {
      return Optional.empty();
    }
    return Optional.of(new Optionalish(parameter, type.rawType, wrapped,
        OF_NULLABLE));
  }

  MethodSpec convenienceOverloadMethod() {
    FieldSpec f = parameter.asField();
    ParameterSpec p = ParameterSpec.builder(wrapped,
        parameter.setterName).build();
    CodeBlock.Builder block = CodeBlock.builder();
    if (wrapper.equals(OPTIONAL_CLASS)) {
      block.addStatement("this.$N = $T.$L($N)", f, wrapper, of, p);
    } else {
      block.addStatement("this.$N = $T.of($N)", f, wrapper, p);
    }
    return MethodSpec.methodBuilder(
        parameter.setterName)
        .addCode(block.build())
        .addStatement("return this")
        .addParameter(p)
        .addModifiers(FINAL)
        .addModifiers(parameter.model.maybePublic())
        .returns(parameter.model.generatedClass)
        .build();
  }

  Optionalish withParameter(Parameter parameter) {
    return new Optionalish(parameter, wrapper, wrapped, of);
  }

  CodeBlock getFieldValue(ParameterSpec builder) {
    FieldSpec field = parameter.asField();
    return CodeBlock.of("$N.$N != null ? $N.$N : $T.empty()",
        builder, field, builder, field, wrapper);
  }

  @Override
  <R, P> R accept(Cases<R, P> cases, P p) {
    return cases.optionalish(this, p);
  }
}
