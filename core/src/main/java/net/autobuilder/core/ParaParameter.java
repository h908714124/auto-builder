package net.autobuilder.core;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.util.List;
import java.util.function.Function;

abstract class ParaParameter {

  static abstract class Cases<R> {

    abstract R parameter(Parameter parameter);

    abstract R collectionish(Collectionish collectionish);

    abstract R optionalish(Optionalish optionalish);
  }

  static <R> Function<ParaParameter, R> asFunction(Cases<R> cases) {
    return p -> p.accept(cases);
  }

  static final Function<ParaParameter, Parameter> AS_PARAMETER =
      asFunction(new Cases<Parameter>() {
        @Override
        Parameter parameter(Parameter parameter) {
          return parameter;
        }

        @Override
        Parameter collectionish(Collectionish collectionish) {
          return collectionish.parameter;
        }

        @Override
        Parameter optionalish(Optionalish optionalish) {
          return optionalish.parameter;
        }
      });

  static final Function<ParaParameter, List<String>> METHOD_NAMES =
      asFunction(new Cases<List<String>>() {
        @Override
        List<String> parameter(Parameter parameter) {
          return singletonList(parameter.setterName);
        }

        @Override
        List<String> collectionish(Collectionish collectionish) {
          return collectionish.hasAccumulator() ?
              asList(collectionish.parameter.setterName, collectionish.accumulatorName()) :
              singletonList(collectionish.parameter.setterName);
        }

        @Override
        List<String> optionalish(Optionalish optionalish) {
          return singletonList(optionalish.parameter.setterName);
        }
      });

  static final Function<ParaParameter, List<String>> FIELD_NAMES =
      asFunction(new Cases<List<String>>() {
        @Override
        List<String> parameter(Parameter parameter) {
          return singletonList(parameter.setterName);
        }

        @Override
        List<String> collectionish(Collectionish collectionish) {
          return collectionish.hasAccumulator() ?
              asList(collectionish.parameter.setterName, collectionish.builderFieldName()) :
              singletonList(collectionish.parameter.setterName);
        }

        @Override
        List<String> optionalish(Optionalish optionalish) {
          return singletonList(optionalish.parameter.setterName);
        }
      });

  static final Function<ParaParameter, ParaParameter> NO_ACCUMULATOR =
      asFunction(new Cases<ParaParameter>() {
        @Override
        ParaParameter parameter(Parameter parameter) {
          return parameter;
        }

        @Override
        ParaParameter collectionish(Collectionish collectionish) {
          return collectionish.noAccumulator();
        }

        @Override
        ParaParameter optionalish(Optionalish optionalish) {
          return optionalish;
        }
      });

  static final Function<ParaParameter, ParaParameter> ORIGINAL_SETTER =
      asFunction(new Cases<ParaParameter>() {
        @Override
        ParaParameter parameter(Parameter parameter) {
          return parameter.originalSetter();
        }

        @Override
        ParaParameter collectionish(Collectionish collectionish) {
          return collectionish.withParameter(collectionish.parameter.originalSetter());
        }

        @Override
        ParaParameter optionalish(Optionalish optionalish) {
          return optionalish.withParameter(optionalish.parameter.originalSetter());
        }
      });

  abstract <R> R accept(Cases<R> cases);
}
