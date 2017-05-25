package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static javax.lang.model.element.Modifier.FINAL;
import static net.autobuilder.core.Util.AS_DECLARED;
import static net.autobuilder.core.Util.AS_TYPE_ELEMENT;
import static net.autobuilder.core.Util.equalsType;

final class Optionalish extends ParaParameter {

  private static final String JAVA_UTIL_OPTIONAL = "java.util.Optional";
  private static final ClassName OPTIONAL_CLASS =
      ClassName.get(Optional.class);

  private static final Map<String, TypeName> OPTIONAL_PRIMITIVES;

  static {
    OPTIONAL_PRIMITIVES = new HashMap<>(3);
    OPTIONAL_PRIMITIVES.put("java.util.OptionalInt", TypeName.INT);
    OPTIONAL_PRIMITIVES.put("java.util.OptionalDouble", TypeName.DOUBLE);
    OPTIONAL_PRIMITIVES.put("java.util.OptionalLong", TypeName.LONG);
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
    TypeMirror type = parameter.variableElement.asType();
    DeclaredType declaredType = type.accept(AS_DECLARED, null);
    if (declaredType == null) {
      return Optional.empty();
    }
    TypeElement typeElement = declaredType.asElement().accept(AS_TYPE_ELEMENT, null);
    if (typeElement == null) {
      return Optional.empty();
    }
    if (declaredType.getTypeArguments().size() > 1) {
      return Optional.empty();
    }
    if (declaredType.getTypeArguments().isEmpty()) {
      TypeName primitive = OPTIONAL_PRIMITIVES.get(typeElement.getQualifiedName().toString());
      return primitive != null ?
          Optional.of(new Optionalish(parameter, ClassName.get(typeElement), primitive, "of")) :
          Optional.empty();
    }
    if (!equalsType(type, JAVA_UTIL_OPTIONAL)) {
      return Optional.empty();
    }
    if (equalsType(declaredType.getTypeArguments().get(0), JAVA_UTIL_OPTIONAL)) {
      return Optional.empty();
    }
    return Optional.of(new Optionalish(parameter, OPTIONAL_CLASS,
        TypeName.get(declaredType.getTypeArguments().get(0)),
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
