package net.autobuilder.core;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import net.autobuilder.core.cases.AsRegularParameterCases;
import net.autobuilder.core.cases.AsSetterParameterCases;
import net.autobuilder.core.cases.CodeInsideSetterCases;
import net.autobuilder.core.cases.ExtraFieldCases;
import net.autobuilder.core.cases.ExtraMethodsCases;
import net.autobuilder.core.cases.ExtractCases;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Represents one parameter of the generated auto-value constructor.
 * Alternatively this can be seen as one property of the associated builder.
 * For each property, the builder has at least one setter method.
 * For some types, there can also be additional convenience methods,
 * like an add method for a list.
 */
public abstract class Parameter {

  static <R> Function<Parameter, R> asFunction(ParamCases<R, Void> cases) {
    return parameter -> parameter.accept(cases, null);
  }

  abstract <R, P> R accept(ParamCases<R, P> cases, P p);

  final RegularParameter asRegularParameter() {
    return AS_REGULAR_PARAMETER.apply(this);
  }

  private static final Function<Parameter, RegularParameter> AS_REGULAR_PARAMETER =
      asFunction(new AsRegularParameterCases());

  public ParameterSpec asSetterParameter() {
    return AS_SETTER_PARAMETER.apply(this);
  }

  private static final Function<Parameter, ParameterSpec> AS_SETTER_PARAMETER =
      asFunction(new AsSetterParameterCases());

  /**
   * Creates the code inside one of the builder's setter methods.
   */
  CodeBlock codeInsideSetter() {
    return CODE_INSIDE_SETTER.apply(this);
  }

  private static final Function<Parameter, CodeBlock> CODE_INSIDE_SETTER =
      asFunction(new CodeInsideSetterCases());

  final CodeBlock extract() {
    return EXTRACT.apply(this);
  }

  private static final Function<Parameter, CodeBlock> EXTRACT =
      asFunction(new ExtractCases());

  final Optional<FieldSpec> extraField() {
    return EXTRA_FIELD.apply(this);
  }

  private static final Function<Parameter, Optional<FieldSpec>> EXTRA_FIELD =
      asFunction(new ExtraFieldCases());

  List<MethodSpec> getExtraMethods(Model model) {
    return accept(EXTRA_METHODS, model);
  }

  private static final ParamCases<List<MethodSpec>, Model> EXTRA_METHODS =
      new ExtraMethodsCases();

  /**
   * Code that de-initialises the builder at the end of the {@code build()} method.
   * This will only be called if the builder is reused.
   */
  CodeBlock cleanupCode() {
    RegularParameter param = asRegularParameter();
    CodeBlock.Builder builder = CodeBlock.builder();
    if (!param.variableElement.asType().getKind().isPrimitive()) {
      builder.addStatement("$N = null", param.asField());
    }
    extraField().ifPresent(field ->
        builder.addStatement("$N = null", field));
    return builder.build();
  }
}
