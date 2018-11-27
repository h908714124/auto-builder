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

  private static final Function<ParaParameter, List<String>> METHOD_NAMES =
      asFunction(new MethodNamesCases());

  private static final Function<ParaParameter, List<String>> FIELD_NAMES =
      asFunction(new FieldNamesCases());

  private static final Function<ParaParameter, ParaParameter> NO_ACCUMULATOR =
      asFunction(new NoAccumulatorCases());

  private static final Function<ParaParameter, ParaParameter> ORIGINAL_SETTER =
      asFunction(new OriginalSetterCases());

  private static class MethodNamesCases implements ParamCases<List<String>, Void> {

    @Override
    public List<String> parameter(Parameter parameter, Void _null) {
      return singletonList(parameter.setterName);
    }

    @Override
    public List<String> collectionish(Collectionish collectionish, Void _null) {
      return asList(collectionish.parameter.setterName, collectionish.accumulatorName());
    }

    @Override
    public List<String> optionalish(Optionalish optionalish, Void _null) {
      return singletonList(optionalish.parameter.setterName);
    }
  }

  private static class FieldNamesCases implements ParamCases<List<String>, Void> {

    @Override
    public List<String> parameter(Parameter parameter, Void _null) {
      return singletonList(parameter.setterName);
    }

    @Override
    public List<String> collectionish(Collectionish collectionish, Void _null) {
      return asList(collectionish.parameter.setterName,
          collectionish.builderFieldName());
    }

    @Override
    public List<String> optionalish(Optionalish optionalish, Void _null) {
      return singletonList(optionalish.parameter.setterName);
    }
  }

  private static class NoAccumulatorCases implements ParamCases<ParaParameter, Void> {

    @Override
    public ParaParameter parameter(Parameter parameter, Void _null) {
      return parameter;
    }

    @Override
    public ParaParameter collectionish(Collectionish collectionish, Void _null) {
      return collectionish.parameter;
    }

    @Override
    public ParaParameter optionalish(Optionalish optionalish, Void _null) {
      return optionalish;
    }
  }

  private static class OriginalSetterCases implements ParamCases<ParaParameter, Void> {

    @Override
    public ParaParameter parameter(Parameter parameter, Void _null) {
      return parameter.originalSetter();
    }

    @Override
    public ParaParameter collectionish(Collectionish collectionish, Void _null) {
      return collectionish.withParameter(collectionish.parameter.originalSetter());
    }

    @Override
    public ParaParameter optionalish(Optionalish optionalish, Void _null) {
      return optionalish.withParameter(optionalish.parameter.originalSetter());
    }
  }
}
