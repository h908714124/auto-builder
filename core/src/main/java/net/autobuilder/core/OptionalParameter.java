package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static javax.lang.model.element.Modifier.FINAL;

public final class OptionalParameter extends Parameter {

  private static final ClassName OPTIONAL_CLASS = ClassName.get(Optional.class);

  private static Optional<TypeMirror> optionalPrimitive(TypeMirror mirror) {
    TypeTool tool = TypeTool.get();
    if (tool.isSameType(OptionalInt.class, mirror)) {
      return Optional.of(tool.getPrimitiveType(TypeKind.INT));
    }
    if (tool.isSameType(OptionalLong.class, mirror)) {
      return Optional.of(tool.getPrimitiveType(TypeKind.LONG));
    }
    if (tool.isSameType(OptionalDouble.class, mirror)) {
      return Optional.of(tool.getPrimitiveType(TypeKind.DOUBLE));
    }
    return Optional.empty();
  }

  private static final String OF = "of";
  private static final String OF_NULLABLE = "ofNullable";

  public final RegularParameter parameter;

  public final ClassName wrapper;

  private final Optional<TypeMirror> wrapped;

  private final String of;

  private OptionalParameter(
      RegularParameter parameter,
      ClassName wrapper,
      Optional<TypeMirror> wrapped,
      String of) {
    this.parameter = parameter;
    this.wrapper = wrapper;
    this.wrapped = wrapped;
    this.of = of;
  }

  private static final class CheckoutResult {
    final DeclaredType declaredType;
    final OptionalParameter parameter;

    CheckoutResult(DeclaredType declaredType, OptionalParameter parameter) {
      this.declaredType = declaredType;
      this.parameter = parameter;
    }
  }

  /**
   * @return an optionalish parameter, if this parameter
   * represents an optional type, or else {@link Optional#empty()}
   */
  static Optional<Parameter> maybeCreate(RegularParameter parameter) {
    return checkout(parameter)
        .map(checkoutResult -> checkoutResult.parameter);
  }

  private static Optional<CheckoutResult> checkout(RegularParameter parameter) {
    TypeMirror type = parameter.variableElement.asType();
    if (type.getKind() != TypeKind.DECLARED) {
      return Optional.empty();
    }
    DeclaredType declaredType = Util.asDeclared(type);
    TypeTool tool = TypeTool.get();
    if (tool.hasWildcards(type)) {
      return Optional.empty();
    }
    Optional<TypeElement> opt = tool.getTypeElement(declaredType);
    if (!opt.isPresent()) {
      return Optional.empty();
    }
    TypeElement typeElement = opt.get();
    if (declaredType.getTypeArguments().isEmpty()) {
      return optionalPrimitive(typeElement.asType())
          .map(prim -> new CheckoutResult(declaredType,
              new OptionalParameter(parameter, ClassName.get(typeElement), Optional.of(prim), OF)));
    }
    if (!tool.isSameErasure(Optional.class, type)) {
      return Optional.empty();
    }
    if (declaredType.getTypeArguments().size() != 1) {
      return Optional.of(new CheckoutResult(declaredType,
          new OptionalParameter(parameter, OPTIONAL_CLASS,
              Optional.empty(),
              OF_NULLABLE)));
    }
    TypeMirror wrapped = declaredType.getTypeArguments().get(0);
    if (tool.isSameErasure(Optional.class, wrapped)) {
      return Optional.of(new CheckoutResult(declaredType,
          new OptionalParameter(parameter, OPTIONAL_CLASS,
              Optional.empty(),
              OF_NULLABLE)));
    }
    return Optional.of(new CheckoutResult(declaredType,
        new OptionalParameter(parameter, OPTIONAL_CLASS,
            Optional.of(wrapped),
            OF_NULLABLE)));
  }

  public Optional<MethodSpec> convenienceOverloadMethod() {
    return wrapped.map(this::_convenienceOverloadMethod);
  }

  private MethodSpec _convenienceOverloadMethod(TypeMirror wrapped) {
    FieldSpec f = parameter.asField();
    ParameterSpec p = ParameterSpec.builder(TypeName.get(wrapped),
        parameter.setterName).build();
    CodeBlock.Builder block = CodeBlock.builder();
    if (wrapper.equals(OPTIONAL_CLASS)) {
      block.addStatement("this.$N = $T.$L($N)", f, wrapper, of, p);
    } else {
      block.addStatement("this.$N = $T.of($N)", f, wrapper, p);
    }
    return MethodSpec.methodBuilder(parameter.setterName)
        .addCode(block.build())
        .addStatement("return this")
        .addParameter(p)
        .addModifiers(FINAL)
        .addModifiers(parameter.maybePublic())
        .returns(parameter.generatedClass)
        .build();
  }

  OptionalParameter withParameter(RegularParameter parameter) {
    return new OptionalParameter(parameter, wrapper, wrapped, of);
  }

  @Override
  <R, P> R accept(ParamCases<R, P> cases, P p) {
    return cases.optionalish(this, p);
  }
}
