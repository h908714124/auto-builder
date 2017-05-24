package net.autobuilder.core;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static net.autobuilder.core.Util.typeArgumentSubtypes;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

abstract class ParaParameter {

  static abstract class Cases<R, P> {

    abstract R parameter(Parameter parameter, P p);

    abstract R collectionish(Collectionish collectionish, P p);

    abstract R optionalish(Optionalish optionalish, P p);
  }

  private static <R> Function<ParaParameter, R> asFunction(Cases<R, Void> cases) {
    return parameter -> parameter.accept(cases, null);
  }

  private static <R, P> BiFunction<ParaParameter, P, R> biFunction(Cases<R, P> cases) {
    return (parameter, p) -> parameter.accept(cases, p);
  }

  static final Function<ParaParameter, Parameter> GET_PARAMETER =
      asFunction(new Cases<Parameter, Void>() {
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
      asFunction(new Cases<List<String>, Void>() {
        @Override
        List<String> parameter(Parameter parameter, Void _null) {
          return singletonList(parameter.setterName);
        }

        @Override
        List<String> collectionish(Collectionish collectionish, Void _null) {
          return collectionish.hasAccumulator() ?
              asList(collectionish.parameter.setterName, collectionish.accumulatorName()) :
              singletonList(collectionish.parameter.setterName);
        }

        @Override
        List<String> optionalish(Optionalish optionalish, Void _null) {
          return singletonList(optionalish.parameter.setterName);
        }
      });

  static final Function<ParaParameter, List<String>> FIELD_NAMES =
      asFunction(new Cases<List<String>, Void>() {
        @Override
        List<String> parameter(Parameter parameter, Void _null) {
          return singletonList(parameter.setterName);
        }

        @Override
        List<String> collectionish(Collectionish collectionish, Void _null) {
          return collectionish.hasAccumulator() ?
              asList(collectionish.parameter.setterName, collectionish.builderFieldName()) :
              singletonList(collectionish.parameter.setterName);
        }

        @Override
        List<String> optionalish(Optionalish optionalish, Void _null) {
          return singletonList(optionalish.parameter.setterName);
        }
      });

  static final Function<ParaParameter, ParaParameter> NO_ACCUMULATOR =
      asFunction(new Cases<ParaParameter, Void>() {
        @Override
        ParaParameter parameter(Parameter parameter, Void _null) {
          return parameter;
        }

        @Override
        ParaParameter collectionish(Collectionish collectionish, Void _null) {
          return collectionish.noAccumulator();
        }

        @Override
        ParaParameter optionalish(Optionalish optionalish, Void _null) {
          return optionalish;
        }
      });

  static final Function<ParaParameter, ParaParameter> ORIGINAL_SETTER =
      asFunction(new Cases<ParaParameter, Void>() {
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

  static final Function<ParaParameter, ParameterSpec> AS_PARAMETER =
      asFunction(new Cases<ParameterSpec, Void>() {
        @Override
        ParameterSpec parameter(Parameter parameter, Void _null) {
          return ParameterSpec.builder(parameter.type, parameter.setterName).build();
        }

        @Override
        ParameterSpec collectionish(Collectionish collectionish, Void _null) {
          TypeName type = collectionish.wildTyping ?
              ParameterizedTypeName.get(collectionish.setterParameterClassName,
                  typeArgumentSubtypes(
                      collectionish.parameter.variableElement)) :
              collectionish.parameter.type;
          return ParameterSpec.builder(type, collectionish.parameter.setterName).build();
        }

        @Override
        ParameterSpec optionalish(Optionalish optionalish, Void _null) {
          return ParameterSpec.builder(optionalish.parameter.type,
              optionalish.parameter.setterName).build();
        }
      });

  static final Function<ParaParameter, CodeBlock> SETTER_ASSIGNMENT =
      asFunction(new Cases<CodeBlock, Void>() {
        @Override
        CodeBlock parameter(Parameter parameter, Void _null) {
          FieldSpec field = parameter.asField();
          ParameterSpec p = AS_PARAMETER.apply(parameter);
          return CodeBlock.builder()
              .addStatement("this.$N = $N", field, p).build();
        }

        @Override
        CodeBlock collectionish(Collectionish collectionish, Void _null) {
          return collectionish.setterAssignment.apply(collectionish);
        }

        @Override
        CodeBlock optionalish(Optionalish optionalish, Void _null) {
          FieldSpec field = optionalish.parameter.asField();
          ParameterSpec p = AS_PARAMETER.apply(optionalish);
          return CodeBlock.builder()
              .addStatement("this.$N = $N", field, p).build();
        }
      });

  static final BiFunction<ParaParameter, ParameterSpec, CodeBlock> GET_FIELD_VALUE_BLOCK =
      biFunction(new Cases<CodeBlock, ParameterSpec>() {
        @Override
        CodeBlock parameter(Parameter parameter, ParameterSpec builder) {
          return CodeBlock.of("$N.$N", builder, parameter.asField());
        }

        @Override
        CodeBlock collectionish(Collectionish collectionish, ParameterSpec builder) {
          FieldSpec field = collectionish.parameter.asField();
          CodeBlock getCollection = CodeBlock.builder()
              .add("$N.$N != null ? $N.$N : ",
                  builder, field, builder, field)
              .add(collectionish.emptyBlock.get())
              .build();
          if (!collectionish.hasAccumulator()) {
            return getCollection;
          }
          FieldSpec builderField = collectionish.asBuilderField();
          return CodeBlock.builder()
              .add("$N.$N != null ? ", builder, builderField)
              .add(collectionish.buildBlock.apply(builder, builderField))
              .add(" :\n        ")
              .add(getCollection)
              .build();
        }

        @Override
        CodeBlock optionalish(Optionalish optionalish, ParameterSpec builder) {
          FieldSpec field = optionalish.parameter.asField();
          return CodeBlock.of("$N.$N != null ? $N.$N : $T.empty()",
              builder, field,
              builder, field,
              optionalish.wrapper);
        }
      });

  static final BiFunction<ParaParameter, CodeBlock.Builder, Void> CLEAR_ACCUMULATOR =
      biFunction(new Cases<Void, CodeBlock.Builder>() {
        @Override
        Void parameter(Parameter parameter, CodeBlock.Builder builder) {
          return null;
        }

        @Override
        Void collectionish(Collectionish collectionish, CodeBlock.Builder block) {
          block.addStatement("this.$N = null",
              collectionish.asBuilderField());
          return null;
        }

        @Override
        Void optionalish(Optionalish optionalish, CodeBlock.Builder builder) {
          return null;
        }
      });


  abstract <R, P> R accept(Cases<R, P> cases, P p);
}
