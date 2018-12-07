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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static javax.lang.model.element.Modifier.FINAL;
import static net.autobuilder.core.Util.equalsType;

public final class Optionalish extends ParaParameter {

  private static final String JAVA_UTIL_OPTIONAL = "java.util.Optional";
  private static final ClassName OPTIONAL_CLASS =
      ClassName.get(Optional.class);

  private static final Map<String, TypeName> OPTIONAL_PRIMITIVES =
      Collections.unmodifiableMap(optionalPrimitives());

  static Map<String, TypeName> optionalPrimitives() {
    Map<String, TypeName> m = new HashMap<>(3);
    m.put(OptionalInt.class.getCanonicalName(), TypeName.INT);
    m.put(OptionalDouble.class.getCanonicalName(), TypeName.DOUBLE);
    m.put(OptionalLong.class.getCanonicalName(), TypeName.LONG);
    return m;
  }

  private static final String OF = "of";
  private static final String OF_NULLABLE = "ofNullable";

  public final Parameter parameter;

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

  private static final class CheckoutResult {
    final DeclaredType declaredType;
    final Optionalish optionalish;

    CheckoutResult(DeclaredType declaredType, Optionalish optionalish) {
      this.declaredType = declaredType;
      this.optionalish = optionalish;
    }
  }

  /**
   * @return an optionalish parameter, if this parameter
   * represents an optional type, or else {@link Optional#empty()}
   */
  static Optional<ParaParameter> maybeCreate(Parameter parameter) {
    return checkout(parameter)
        .filter(checkoutResult -> checkoutResult.optionalish.wrapped.isPrimitive() ||
            !equalsType(checkoutResult.declaredType.getTypeArguments().get(0),
                JAVA_UTIL_OPTIONAL))
        .map(checkoutResult -> checkoutResult.optionalish);
  }

  public static Optional<CodeBlock> emptyBlock(Parameter parameter) {
    return checkout(parameter)
        .map(checkoutResult -> checkoutResult.optionalish)
        .map(Optionalish::getFieldValue);
  }

  public CodeBlock getFieldValue() {
    FieldSpec field = parameter.asField();
    return CodeBlock.of("$N != null ? $N : $T.empty()",
        field, field, wrapper);
  }

  private static Optional<CheckoutResult> checkout(Parameter parameter) {
    TypeMirror type = parameter.variableElement.asType();
    if (type.getKind() != TypeKind.DECLARED) {
      return Optional.empty();
    }
    DeclaredType declaredType = Util.asDeclared(type);
    if (declaredType.getTypeArguments().size() > 1) {
      return Optional.empty();
    }
    Optional<TypeElement> opt = TypeTool.get().getTypeElement(declaredType);
    if (!opt.isPresent()) {
      return Optional.empty();
    }
    TypeElement typeElement = opt.get();
    if (declaredType.getTypeArguments().isEmpty()) {
      TypeName primitive = OPTIONAL_PRIMITIVES.get(typeElement.getQualifiedName().toString());
      return primitive != null ?
          Optional.of(new CheckoutResult(declaredType,
              new Optionalish(parameter, ClassName.get(typeElement), primitive, OF))) :
          Optional.empty();
    }
    return equalsType(type, JAVA_UTIL_OPTIONAL) ?
        Optional.of(new CheckoutResult(declaredType,
            new Optionalish(parameter, OPTIONAL_CLASS,
                TypeName.get(declaredType.getTypeArguments().get(0)),
                OF_NULLABLE))) :
        Optional.empty();
  }

  public MethodSpec convenienceOverloadMethod() {
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

  @Override
  <R, P> R accept(ParamCases<R, P> cases, P p) {
    return cases.optionalish(this, p);
  }
}
