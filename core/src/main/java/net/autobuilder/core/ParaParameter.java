package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

abstract class ParaParameter {

  static abstract class Cases<R, P> {

    abstract R parameter(Parameter parameter, P p);

    abstract R collectionish(Collectionish collectionish, P p);

    abstract R optionalish(Optionalish optionalish, P p);
  }

  private static <R> Function<ParaParameter, R> asFunction(Cases<R, Void> cases) {
    return parameter -> parameter.accept(cases, null);
  }

  private static <P> BiConsumer<ParaParameter, P> asConsumer(Cases<Void, P> cases) {
    return (parameter, p) -> parameter.accept(cases, p);
  }

  private static <R, P> BiFunction<ParaParameter, P, R> biFunction(Cases<R, P> cases) {
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

  static final Function<ParaParameter, List<String>> METHOD_NAMES =
      asFunction(new ParaParameter.Cases<List<String>, Void>() {
        @Override
        List<String> parameter(Parameter parameter, Void _null) {
          return singletonList(parameter.setterName);
        }

        @Override
        List<String> collectionish(Collectionish collectionish, Void _null) {
          return asList(collectionish.parameter.setterName, collectionish.accumulatorName());
        }

        @Override
        List<String> optionalish(Optionalish optionalish, Void _null) {
          return singletonList(optionalish.parameter.setterName);
        }
      });

  static final Function<ParaParameter, List<String>> FIELD_NAMES =
      asFunction(new ParaParameter.Cases<List<String>, Void>() {
        @Override
        List<String> parameter(Parameter parameter, Void _null) {
          return singletonList(parameter.setterName);
        }

        @Override
        List<String> collectionish(Collectionish collectionish, Void _null) {
          return asList(collectionish.parameter.setterName,
              collectionish.builderFieldName());
        }

        @Override
        List<String> optionalish(Optionalish optionalish, Void _null) {
          return singletonList(optionalish.parameter.setterName);
        }
      });

  static final Function<ParaParameter, ParaParameter> NO_ACCUMULATOR =
      asFunction(new ParaParameter.Cases<ParaParameter, Void>() {
        @Override
        ParaParameter parameter(Parameter parameter, Void _null) {
          return parameter;
        }

        @Override
        ParaParameter collectionish(Collectionish collectionish, Void _null) {
          return collectionish.parameter;
        }

        @Override
        ParaParameter optionalish(Optionalish optionalish, Void _null) {
          return optionalish;
        }
      });

  static final Function<ParaParameter, ParaParameter> ORIGINAL_SETTER =
      asFunction(new ParaParameter.Cases<ParaParameter, Void>() {
        @Override
        ParaParameter parameter(Parameter parameter, Void _null) {
          return parameter.originalSetter();
        }

        @Override
        ParaParameter collectionish(Collectionish collectionish, Void _null) {
          return collectionish.withParameter(collectionish.parameter.originalSetter());
        }

        @Override
        ParaParameter optionalish(Optionalish optionalish, Void _null) {
          return optionalish.withParameter(optionalish.parameter.originalSetter());
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

  static final BiFunction<ParaParameter, ParameterSpec, CodeBlock> GET_FIELD_VALUE =
      biFunction(new ParaParameter.Cases<CodeBlock, ParameterSpec>() {
        @Override
        CodeBlock parameter(Parameter parameter, ParameterSpec builder) {
          return Collectionish.emptyBlock(parameter, builder)
              .orElse(Optionalish.emptyBlock(parameter, builder)
                  .orElse(CodeBlock.of("$N.$N", builder, parameter.asField())));
        }

        @Override
        CodeBlock collectionish(Collectionish collectionish, ParameterSpec builder) {
          return collectionish.getFieldValue(builder);
        }

        @Override
        CodeBlock optionalish(Optionalish optionalish, ParameterSpec builder) {
          return optionalish.getFieldValue(builder);
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

  static final BiFunction<ParaParameter, ParameterSpec, Optional<CodeBlock>> CLEANUP_CODE =
      biFunction(new ParaParameter.Cases<Optional<CodeBlock>, ParameterSpec>() {
        @Override
        Optional<CodeBlock> parameter(Parameter parameter, ParameterSpec builder) {
          if (parameter.type instanceof ClassName ||
              parameter.type instanceof ParameterizedTypeName) {
            return Optional.of(CodeBlock.builder().addStatement("$N.$L(null)",
                builder, parameter.setterName).build());
          }
          return Optional.empty();
        }
        @Override
        Optional<CodeBlock> collectionish(Collectionish collectionish, ParameterSpec builder) {
          return this.parameter(collectionish.parameter, builder);
        }
        @Override
        Optional<CodeBlock> optionalish(Optionalish optionalish, ParameterSpec builder) {
          return Optional.of(CodeBlock.builder().addStatement("$N.$L(($T) null)",
              builder, optionalish.parameter.setterName,
              optionalish.parameter.type).build());
        }
      });

}
