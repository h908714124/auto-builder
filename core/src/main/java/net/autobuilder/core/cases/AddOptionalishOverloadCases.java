package net.autobuilder.core.cases;

import com.squareup.javapoet.TypeSpec;
import net.autobuilder.core.ParamCases;
import net.autobuilder.core.Collectionish;
import net.autobuilder.core.Optionalish;
import net.autobuilder.core.Parameter;

public class AddOptionalishOverloadCases implements ParamCases<Void, TypeSpec.Builder> {
  @Override
  public Void parameter(Parameter parameter, TypeSpec.Builder builder) {
    return null;
  }

  @Override
  public Void collectionish(Collectionish collectionish, TypeSpec.Builder block) {
    return null;
  }

  @Override
  public Void optionalish(Optionalish optionalish, TypeSpec.Builder builder) {
    builder.addMethod(optionalish.convenienceOverloadMethod());
    return null;
  }
}
