package net.autobuilder.core;

import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static net.autobuilder.core.ParaParameter.asFunction;
import static net.autobuilder.core.Util.isDistinct;

final class Cleanse {

  static List<ParaParameter> detox(List<ParaParameter> parameters) {
    if (!parameters.stream()
        .map(FIELD_NAMES)
        .map(List::stream)
        .flatMap(Function.identity())
        .collect(isDistinct()) ||
        !parameters.stream()
            .map(METHOD_NAMES)
            .map(List::stream)
            .flatMap(Function.identity())
            .collect(isDistinct())) {
      parameters = parameters.stream()
          .map(NO_ACCUMULATOR)
          .collect(toList());
    }
    if (!parameters.stream()
        .map(METHOD_NAMES)
        .map(List::stream)
        .flatMap(Function.identity())
        .collect(isDistinct())) {
      parameters = parameters.stream()
          .map(ORIGINAL_SETTER)
          .collect(toList());
    }
    return parameters;
  }

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

}
