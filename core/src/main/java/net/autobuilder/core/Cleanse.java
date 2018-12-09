package net.autobuilder.core;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static net.autobuilder.core.Parameter.asFunction;
import static net.autobuilder.core.Util.isDistinct;

final class Cleanse {

  /**
   * Simplify the generated builder, if necessary because of potential name collisions.
   */
  static List<Parameter> preventNamingCollisions(List<Parameter> parameters) {
    Boolean areFieldNamesDistinct = parameters.stream().map(FIELD_NAMES).collect(isDistinct());
    if (areFieldNamesDistinct) {
      return parameters;
    }
    return parameters.stream().map(ORIGINAL_SETTER).collect(toList());
  }

  private static final Function<Parameter, Stream<String>> FIELD_NAMES =
      asFunction(new FieldNamesCases());

  private static final Function<Parameter, Parameter> ORIGINAL_SETTER =
      asFunction(new OriginalSetterCases());

  private static class FieldNamesCases implements ParamCases<Stream<String>, Void> {

    @Override
    public Stream<String> parameter(RegularParameter parameter, Void _null) {
      return Stream.of(parameter.setterName);
    }

    @Override
    public Stream<String> collectionish(CollectionParameter parameter, Void _null) {
      return Stream.of(parameter.parameter.setterName,
          parameter.builderFieldName());
    }

    @Override
    public Stream<String> optionalish(OptionalParameter parameter, Void _null) {
      return Stream.of(parameter.parameter.setterName);
    }
  }

  private static class OriginalSetterCases implements ParamCases<Parameter, Void> {

    @Override
    public Parameter parameter(RegularParameter parameter, Void _null) {
      return parameter.originalSetter();
    }

    @Override
    public Parameter collectionish(CollectionParameter parameter, Void _null) {
      return parameter.withParameter(parameter.parameter.originalSetter());
    }

    @Override
    public Parameter optionalish(OptionalParameter parameter, Void _null) {
      return parameter.withParameter(parameter.parameter.originalSetter());
    }
  }
}
