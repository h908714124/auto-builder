package net.autobuilder.core;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static net.autobuilder.core.ParaParameter.asFunction;
import static net.autobuilder.core.Util.isDistinct;

final class Cleanse {

  /**
   * Simplify the generated builder, if necessary because of potential name collisions.
   */
  static List<ParaParameter> preventNamingCollisions(List<ParaParameter> parameters) {
    Boolean areFieldNamesDistinct = parameters.stream().map(FIELD_NAMES).collect(isDistinct());
    if (areFieldNamesDistinct) {
      return parameters;
    }
    return parameters.stream().map(ORIGINAL_SETTER).collect(toList());
  }

  private static final Function<ParaParameter, Stream<String>> FIELD_NAMES =
      asFunction(new FieldNamesCases());

  private static final Function<ParaParameter, ParaParameter> ORIGINAL_SETTER =
      asFunction(new OriginalSetterCases());

  private static class FieldNamesCases implements ParamCases<Stream<String>, Void> {

    @Override
    public Stream<String> parameter(Parameter parameter, Void _null) {
      return Stream.of(parameter.setterName);
    }

    @Override
    public Stream<String> collectionish(Collectionish collectionish, Void _null) {
      return Stream.of(collectionish.parameter.setterName,
          collectionish.builderFieldName());
    }

    @Override
    public Stream<String> optionalish(Optionalish optionalish, Void _null) {
      return Stream.of(optionalish.parameter.setterName);
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
