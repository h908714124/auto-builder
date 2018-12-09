package net.autobuilder.core;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import net.autobuilder.core.cases.AsSetterParameterCases;
import net.autobuilder.core.cases.CleanupCodeCases;
import net.autobuilder.core.cases.ClearAccumulatorCases;
import net.autobuilder.core.cases.ExtraFieldCases;
import net.autobuilder.core.cases.ExtraMethodsCases;
import net.autobuilder.core.cases.GetFieldValueCases;
import net.autobuilder.core.cases.GetParameterCases;
import net.autobuilder.core.cases.SetterAssignmentCases;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Represents one parameter of the generated auto-value constructor
 */
public abstract class ParaParameter {

  static <R> Function<ParaParameter, R> asFunction(ParamCases<R, Void> cases) {
    return parameter -> parameter.accept(cases, null);
  }

  private static <P> BiConsumer<ParaParameter, P> asConsumer(ParamCases<Void, P> cases) {
    return (parameter, p) -> parameter.accept(cases, p);
  }

  abstract <R, P> R accept(ParamCases<R, P> cases, P p);

  final Parameter getParameter() {
    return GET_PARAMETER.apply(this);
  }

  private static final Function<ParaParameter, Parameter> GET_PARAMETER =
      asFunction(new GetParameterCases());

  public ParameterSpec asSetterParameter() {
    return AS_SETTER_PARAMETER.apply(this);
  }

  private static final Function<ParaParameter, ParameterSpec> AS_SETTER_PARAMETER =
      asFunction(new AsSetterParameterCases());

  CodeBlock setterAssignment() {
    return SETTER_ASSIGNMENT.apply(this);
  }

  private static final Function<ParaParameter, CodeBlock> SETTER_ASSIGNMENT =
      asFunction(new SetterAssignmentCases());

  final CodeBlock fieldValue() {
    return GET_FIELD_VALUE.apply(this);
  }

  private static final Function<ParaParameter, CodeBlock> GET_FIELD_VALUE =
      asFunction(new GetFieldValueCases());

  void clearAccumulator(CodeBlock.Builder builder) {
    CLEAR_ACCUMULATOR.accept(this, builder);
  }

  private static final BiConsumer<ParaParameter, CodeBlock.Builder> CLEAR_ACCUMULATOR =
      asConsumer(new ClearAccumulatorCases());

  Optional<FieldSpec> getExtraField() {
    return EXTRA_FIELD.apply(this);
  }

  private static final Function<ParaParameter, Optional<FieldSpec>> EXTRA_FIELD =
      asFunction(new ExtraFieldCases());

  List<MethodSpec> getExtraMethods(Model model) {
    return accept(EXTRA_METHODS, model);
  }

  private static final ParamCases<List<MethodSpec>, Model> EXTRA_METHODS =
      new ExtraMethodsCases();

  void cleanupCode(CodeBlock.Builder builder) {
    CLEANUP_CODE.accept(this, builder);
  }

  private static final BiConsumer<ParaParameter, CodeBlock.Builder> CLEANUP_CODE =
      asConsumer(new CleanupCodeCases());
}
