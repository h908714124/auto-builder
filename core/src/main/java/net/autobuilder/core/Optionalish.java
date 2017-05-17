package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static net.autobuilder.core.AutoBuilderProcessor.rawType;

final class Optionalish {

  static final ClassName OPTIONAL_CLASS =
      ClassName.get(Optional.class);

  private static final List<Optionalish> OPTIONAL_PRIMITIVES =
      Arrays.asList(
          new Optionalish(ClassName.get(OptionalInt.class), TypeName.INT),
          new Optionalish(ClassName.get(OptionalDouble.class), TypeName.DOUBLE),
          new Optionalish(ClassName.get(OptionalLong.class), TypeName.LONG));

  final ClassName wrapper;
  final TypeName wrapped;

  private Optionalish(ClassName wrapper, TypeName wrapped) {
    this.wrapper = wrapper;
    this.wrapped = wrapped;
  }

  static Optionalish create(TypeName typeName) {
    if (typeName instanceof ClassName) {
      for (Optionalish optionalPrimitive : OPTIONAL_PRIMITIVES) {
        if (optionalPrimitive.wrapper.equals(typeName)) {
          return optionalPrimitive;
        }
      }
      return null;
    }
    if (!(typeName instanceof ParameterizedTypeName)) {
      return null;
    }
    ParameterizedTypeName type = (ParameterizedTypeName) typeName;
    if (!type.rawType.equals(OPTIONAL_CLASS)) {
      return null;
    }
    TypeName wrapped = type.typeArguments.get(0);
    if (rawType(wrapped).equals(OPTIONAL_CLASS)) {
      return null;
    }
    return new Optionalish(OPTIONAL_CLASS, wrapped);
  }

  boolean isOptional() {
    return wrapper.equals(OPTIONAL_CLASS);
  }
}
