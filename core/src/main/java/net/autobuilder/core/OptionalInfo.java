package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static net.autobuilder.core.AutoBuilderProcessor.rawType;

final class OptionalInfo {

  private static final ClassName OPTIONAL_CLASS =
      ClassName.get(Optional.class);

  private static final List<OptionalInfo> OPTIONAL_PRIMITIVES =
      Arrays.asList(
          new OptionalInfo(ClassName.get(OptionalInt.class), TypeName.INT),
          new OptionalInfo(ClassName.get(OptionalDouble.class), TypeName.DOUBLE),
          new OptionalInfo(ClassName.get(OptionalLong.class), TypeName.LONG));

  final ClassName wrapper;
  final TypeName wrapped;

  private OptionalInfo(ClassName wrapper, TypeName wrapped) {
    this.wrapper = wrapper;
    this.wrapped = wrapped;
  }

  static boolean isOptionalPrimitive(TypeName typeName) {
    if (!(typeName instanceof ClassName)) {
      return false;
    }
    for (OptionalInfo optionalPrimitive : OPTIONAL_PRIMITIVES) {
      if (optionalPrimitive.wrapper.equals(typeName)) {
        return true;
      }
    }
    return false;
  }

  static OptionalInfo create(TypeName typeName) {
    if (typeName instanceof ClassName) {
      for (OptionalInfo optionalPrimitive : OPTIONAL_PRIMITIVES) {
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
    return new OptionalInfo(OPTIONAL_CLASS,
        type.typeArguments.get(0));
  }

  static boolean isOptional(TypeName typeName) {
    return typeName instanceof ParameterizedTypeName &&
        rawType(typeName).equals(OPTIONAL_CLASS);
  }

  boolean isDoubleOptional() {
    return wrapped instanceof TypeVariableName ||
        wrapper.equals(OPTIONAL_CLASS) &&
            rawType(wrapped).equals(OPTIONAL_CLASS);
  }
}
