package net.autobuilder.core.cases;

import com.squareup.javapoet.FieldSpec;
import net.autobuilder.core.Collectionish;
import net.autobuilder.core.Optionalish;
import net.autobuilder.core.ParamCases;
import net.autobuilder.core.Parameter;

import java.util.Optional;

public class ExtraFieldCases implements ParamCases<Optional<FieldSpec>, Void> {

  @Override
  public Optional<FieldSpec> parameter(Parameter parameter, Void _null) {
    return Optional.empty();
  }

  @Override
  public Optional<FieldSpec> collectionish(Collectionish collectionish, Void _null) {
    return Optional.of(collectionish.asBuilderField());
  }

  @Override
  public Optional<FieldSpec> optionalish(Optionalish optionalish, Void _null) {
    return Optional.empty();
  }
}
