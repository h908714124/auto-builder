package net.autobuilder.core.cases;

import com.squareup.javapoet.TypeSpec;
import net.autobuilder.core.ParamCases;
import net.autobuilder.core.Collectionish;
import net.autobuilder.core.Optionalish;
import net.autobuilder.core.Parameter;

public class AddAccumulatorFieldCases implements ParamCases<Void, TypeSpec.Builder> {
  @Override
  public Void parameter(Parameter parameter, TypeSpec.Builder builder) {
    return null;
  }

  @Override
  public Void collectionish(Collectionish collectionish, TypeSpec.Builder builder) {
    builder.addField(collectionish.asBuilderField());
    return null;
  }

  @Override
  public Void optionalish(Optionalish optionalish, TypeSpec.Builder builder) {
    return null;
  }
}
