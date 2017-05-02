package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

final class OptionalInfo {

  private static final ClassName OPTIONAL_CLASS =
      ClassName.get(Optional.class);
  private static final OptionalInfo OPTIONAL_INT_INFO =
      new OptionalInfo(ClassName.get(OptionalInt.class), TypeName.INT);
  private static final OptionalInfo OPTIONAL_DOUBLE_INFO =
      new OptionalInfo(ClassName.get(OptionalDouble.class), TypeName.DOUBLE);
  private static final OptionalInfo OPTIONAL_LONG_INFO =
      new OptionalInfo(ClassName.get(OptionalLong.class), TypeName.LONG);

  final ClassName wrapper;
  final TypeName wrapped;

  private OptionalInfo(ClassName wrapper, TypeName wrapped) {
    this.wrapper = wrapper;
    this.wrapped = wrapped;
  }

  static OptionalInfo create(TypeName typeName) {
    if (typeName.isPrimitive()) {
      return null;
    }
    if (typeName instanceof ClassName) {
      if (OPTIONAL_DOUBLE_INFO.wrapper.equals(typeName)) {
        return OPTIONAL_DOUBLE_INFO;
      }
      if (OPTIONAL_INT_INFO.wrapper.equals(typeName)) {
        return OPTIONAL_INT_INFO;
      }
      if (OPTIONAL_LONG_INFO.wrapper.equals(typeName)) {
        return OPTIONAL_LONG_INFO;
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
}
