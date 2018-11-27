package net.autobuilder.core;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import net.autobuilder.core.cases.AddAccumulatorFieldCases;
import net.autobuilder.core.cases.AddAccumulatorMethodCases;
import net.autobuilder.core.cases.AddAccumulatorOverloadCases;
import net.autobuilder.core.cases.AddOptionalishOverloadCases;
import net.autobuilder.core.cases.AsSetterParameterCases;
import net.autobuilder.core.cases.CleanupCodeCases;
import net.autobuilder.core.cases.ClearAccumulatorCases;
import net.autobuilder.core.cases.GetFieldValueCases;
import net.autobuilder.core.cases.GetParameterCases;
import net.autobuilder.core.cases.SetterAssignmentCases;

import java.util.function.BiConsumer;
import java.util.function.Function;

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

  CodeBlock getFieldValue() {
    return GET_FIELD_VALUE.apply(this);
  }

  private static final Function<ParaParameter, CodeBlock> GET_FIELD_VALUE =
      asFunction(new GetFieldValueCases());

  void clearAccumulator(CodeBlock.Builder builder) {
    CLEAR_ACCUMULATOR.accept(this, builder);
  }

  private static final BiConsumer<ParaParameter, CodeBlock.Builder> CLEAR_ACCUMULATOR =
      asConsumer(new ClearAccumulatorCases());

  void addOptionalishOverload(TypeSpec.Builder builder) {
    ADD_OPTIONALISH_OVERLOAD.accept(this, builder);
  }

  private static final BiConsumer<ParaParameter, TypeSpec.Builder> ADD_OPTIONALISH_OVERLOAD =
      asConsumer(new AddOptionalishOverloadCases());

  void addAccumulatorField(TypeSpec.Builder builder) {
    ADD_ACCUMULATOR_FIELD.accept(this, builder);
  }

  private static final BiConsumer<ParaParameter, TypeSpec.Builder> ADD_ACCUMULATOR_FIELD =
      asConsumer(new AddAccumulatorFieldCases());

  void addAccumulatorMethod(TypeSpec.Builder builder) {
    ADD_ACCUMULATOR_METHOD.accept(this, builder);
  }

  private static final BiConsumer<ParaParameter, TypeSpec.Builder> ADD_ACCUMULATOR_METHOD =
      asConsumer(new AddAccumulatorMethodCases());

  void addAccumulatorOverload(TypeSpec.Builder builder) {
    ADD_ACCUMULATOR_OVERLOAD.accept(this, builder);
  }

  private static final BiConsumer<ParaParameter, TypeSpec.Builder> ADD_ACCUMULATOR_OVERLOAD =
      asConsumer(new AddAccumulatorOverloadCases());

  void cleanupCode(CodeBlock.Builder builder) {
    CLEANUP_CODE.accept(this, builder);
  }

  private static final BiConsumer<ParaParameter, CodeBlock.Builder> CLEANUP_CODE =
      asConsumer(new CleanupCodeCases());
}
