package net.autobuilder.core;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.function.BiConsumer;
import java.util.function.Function;

abstract class ParaParameter {

  static abstract class Cases<R, P> {

    abstract R parameter(Parameter parameter, P p);

    abstract R collectionish(Collectionish collectionish, P p);

    abstract R optionalish(Optionalish optionalish, P p);
  }

  static <R> Function<ParaParameter, R> asFunction(Cases<R, Void> cases) {
    return parameter -> parameter.accept(cases, null);
  }

  private static <P> BiConsumer<ParaParameter, P> asConsumer(Cases<Void, P> cases) {
    return (parameter, p) -> parameter.accept(cases, p);
  }

  abstract <R, P> R accept(Cases<R, P> cases, P p);


  static final Function<ParaParameter, Parameter> GET_PARAMETER =
      asFunction(new ParaParameter.Cases<Parameter, Void>() {
        @Override
        Parameter parameter(Parameter parameter, Void _null) {
          return parameter;
        }

        @Override
        Parameter collectionish(Collectionish collectionish, Void _null) {
          return collectionish.parameter;
        }

        @Override
        Parameter optionalish(Optionalish optionalish, Void _null) {
          return optionalish.parameter;
        }
      });


  static final Function<ParaParameter, ParameterSpec> AS_SETTER_PARAMETER =
      asFunction(new ParaParameter.Cases<ParameterSpec, Void>() {
        @Override
        ParameterSpec parameter(Parameter parameter, Void _null) {
          return ParameterSpec.builder(parameter.type, parameter.setterName).build();
        }

        @Override
        ParameterSpec collectionish(Collectionish collectionish, Void _null) {
          return collectionish.asSetterParameter();
        }

        @Override
        ParameterSpec optionalish(Optionalish optionalish, Void _null) {
          return ParameterSpec.builder(optionalish.parameter.type,
              optionalish.parameter.setterName).build();
        }
      });

  static final Function<ParaParameter, CodeBlock> SETTER_ASSIGNMENT =
      asFunction(new ParaParameter.Cases<CodeBlock, Void>() {
        @Override
        CodeBlock parameter(Parameter parameter, Void _null) {
          FieldSpec field = parameter.asField();
          ParameterSpec p = AS_SETTER_PARAMETER.apply(parameter);
          return CodeBlock.builder()
              .addStatement("this.$N = $N", field, p).build();
        }

        @Override
        CodeBlock collectionish(Collectionish collectionish, Void _null) {
          return collectionish.setterAssignment();
        }

        @Override
        CodeBlock optionalish(Optionalish optionalish, Void _null) {
          FieldSpec field = optionalish.parameter.asField();
          ParameterSpec p = AS_SETTER_PARAMETER.apply(optionalish);
          return CodeBlock.builder()
              .addStatement("this.$N = $N", field, p).build();
        }
      });

  static final Function<ParaParameter, CodeBlock> GET_FIELD_VALUE =
      asFunction(new ParaParameter.Cases<CodeBlock, Void>() {
        @Override
        CodeBlock parameter(Parameter parameter, Void _null) {
          return Collectionish.emptyBlock(parameter)
              .orElse(Optionalish.emptyBlock(parameter)
                  .orElse(CodeBlock.of("$N.$N",
                      parameter.model.builderParameter(),
                      parameter.asField())));
        }

        @Override
        CodeBlock collectionish(Collectionish collectionish, Void _null) {
          return collectionish.getFieldValue();
        }

        @Override
        CodeBlock optionalish(Optionalish optionalish, Void _null) {
          return optionalish.getFieldValue();
        }
      });

  static final BiConsumer<ParaParameter, CodeBlock.Builder> CLEAR_ACCUMULATOR =
      asConsumer(new ParaParameter.Cases<Void, CodeBlock.Builder>() {
        @Override
        Void parameter(Parameter parameter, CodeBlock.Builder builder) {
          return null;
        }

        @Override
        Void collectionish(Collectionish collectionish, CodeBlock.Builder builder) {
          builder.addStatement("this.$N = null",
              collectionish.asBuilderField());
          return null;
        }

        @Override
        Void optionalish(Optionalish optionalish, CodeBlock.Builder builder) {
          return null;
        }
      });

  static final BiConsumer<ParaParameter, TypeSpec.Builder> ADD_OPTIONALISH_OVERLOAD =
      asConsumer(new ParaParameter.Cases<Void, TypeSpec.Builder>() {
        @Override
        Void parameter(Parameter parameter, TypeSpec.Builder builder) {
          return null;
        }

        @Override
        Void collectionish(Collectionish collectionish, TypeSpec.Builder block) {
          return null;
        }

        @Override
        Void optionalish(Optionalish optionalish, TypeSpec.Builder builder) {
          builder.addMethod(optionalish.convenienceOverloadMethod());
          return null;
        }
      });

  static final BiConsumer<ParaParameter, TypeSpec.Builder> ADD_ACCUMULATOR_FIELD =
      asConsumer(new ParaParameter.Cases<Void, TypeSpec.Builder>() {
        @Override
        Void parameter(Parameter parameter, TypeSpec.Builder builder) {
          return null;
        }

        @Override
        Void collectionish(Collectionish collectionish, TypeSpec.Builder builder) {
          builder.addField(collectionish.asBuilderField());
          return null;
        }

        @Override
        Void optionalish(Optionalish optionalish, TypeSpec.Builder builder) {
          return null;
        }
      });

  static final BiConsumer<ParaParameter, TypeSpec.Builder> ADD_ACCUMULATOR_METHOD =
      asConsumer(new ParaParameter.Cases<Void, TypeSpec.Builder>() {
        @Override
        Void parameter(Parameter parameter, TypeSpec.Builder builder) {
          return null;
        }

        @Override
        Void collectionish(Collectionish collectionish, TypeSpec.Builder builder) {
          builder.addMethod(collectionish.accumulatorMethod());
          return null;
        }

        @Override
        Void optionalish(Optionalish optionalish, TypeSpec.Builder builder) {
          return null;
        }
      });

  static final BiConsumer<ParaParameter, TypeSpec.Builder> ADD_ACCUMULATOR_OVERLOAD =
      asConsumer(new ParaParameter.Cases<Void, TypeSpec.Builder>() {
        @Override
        Void parameter(Parameter parameter, TypeSpec.Builder builder) {
          return null;
        }

        @Override
        Void collectionish(Collectionish collectionish, TypeSpec.Builder builder) {
          builder.addMethod(collectionish.accumulatorMethodOverload());
          return null;
        }

        @Override
        Void optionalish(Optionalish optionalish, TypeSpec.Builder builder) {
          return null;
        }
      });

  static final BiConsumer<ParaParameter, CodeBlock.Builder> CLEANUP_CODE =
      asConsumer(new ParaParameter.Cases<Void, CodeBlock.Builder>() {
        @Override
        Void parameter(Parameter parameter, CodeBlock.Builder builder) {
          if (!parameter.variableElement.asType().getKind().isPrimitive()) {
            builder.addStatement("$N.$L(null)",
                parameter.model.builderParameter(), parameter.setterName);
          }
          return null;
        }

        @Override
        Void collectionish(Collectionish collectionish, CodeBlock.Builder builder) {
          builder.addStatement("$N.$L(null)",
              collectionish.parameter.model.builderParameter(),
              collectionish.parameter.setterName);
          return null;
        }

        @Override
        Void optionalish(Optionalish optionalish, CodeBlock.Builder builder) {
          builder.addStatement("$N.$L(($T) null)",
              optionalish.parameter.model.builderParameter(),
              optionalish.parameter.setterName,
              optionalish.parameter.type);
          return null;
        }
      });
}
