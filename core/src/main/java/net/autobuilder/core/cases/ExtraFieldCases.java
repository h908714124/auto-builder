package net.autobuilder.core.cases;

import com.squareup.javapoet.FieldSpec;
import net.autobuilder.core.CollectionParameter;
import net.autobuilder.core.OptionalParameter;
import net.autobuilder.core.ParamCases;
import net.autobuilder.core.RegularParameter;

import java.util.Optional;

/**
 * Gets the accumulator field for one builder parameter, if any.
 */
public class ExtraFieldCases implements ParamCases<Optional<FieldSpec>, Void> {

  @Override
  public Optional<FieldSpec> parameter(RegularParameter parameter, Void _null) {
    return Optional.empty();
  }

  @Override
  public Optional<FieldSpec> collectionish(CollectionParameter parameter, Void _null) {
    return parameter.asBuilderField();
  }

  @Override
  public Optional<FieldSpec> optionalish(OptionalParameter parameter, Void _null) {
    return Optional.empty();
  }
}
