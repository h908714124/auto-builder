package net.autobuilder.core;

import static net.autobuilder.core.AutoBuilderProcessor.rawType;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

final class Optionalish extends ParaParameter {

  static final ClassName OPTIONAL_CLASS =
      ClassName.get(Optional.class);

  private static final List<Optionalish> OPTIONAL_PRIMITIVES =
      Arrays.asList(
          new Optionalish(null, ClassName.get(OptionalInt.class), TypeName.INT, "of", true),
          new Optionalish(null, ClassName.get(OptionalDouble.class), TypeName.DOUBLE, "of", true),
          new Optionalish(null, ClassName.get(OptionalLong.class), TypeName.LONG, "of", true));
  private static final String OF_NULLABLE = "ofNullable";

  final Parameter parameter;
  final ClassName wrapper;
  final TypeName wrapped;
  final String of;
  private final boolean convenienceOverload;

  private Optionalish(
      Parameter parameter,
      ClassName wrapper,
      TypeName wrapped,
      String of,
      boolean convenienceOverload) {
    this.parameter = parameter;
    this.wrapper = wrapper;
    this.wrapped = wrapped;
    this.of = of;
    this.convenienceOverload = convenienceOverload;
  }

  static Optional<ParaParameter> create(Parameter parameter) {
    if (parameter.type instanceof ClassName) {
      for (Optionalish optionalPrimitive : OPTIONAL_PRIMITIVES) {
        if (optionalPrimitive.wrapper.equals(parameter.type)) {
          return Optional.of(optionalPrimitive.withParameter(parameter));
        }
      }
      return Optional.empty();
    }
    if (!(parameter.type instanceof ParameterizedTypeName)) {
      return Optional.empty();
    }
    ParameterizedTypeName type = (ParameterizedTypeName) parameter.type;
    if (!type.rawType.equals(OPTIONAL_CLASS)) {
      return Optional.empty();
    }
    TypeName wrapped = type.typeArguments.get(0);
    boolean dangerous = rawType(wrapped).equals(OPTIONAL_CLASS);
    return Optional.of(new Optionalish(parameter, type.rawType, wrapped,
        OF_NULLABLE, !dangerous));
  }

  boolean isOptional() {
    return wrapper.equals(OPTIONAL_CLASS);
  }

  boolean convenienceOverload() {
    return convenienceOverload;
  }

  Optionalish withParameter(Parameter parameter) {
    return new Optionalish(parameter, wrapper, wrapped, of, convenienceOverload);
  }

  @Override
  <R> R accept(Cases<R> cases) {
    return cases.optionalish(this);
  }
}
