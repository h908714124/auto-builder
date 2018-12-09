package net.autobuilder.core.cases;

import com.squareup.javapoet.CodeBlock;
import net.autobuilder.core.CollectionParameter;
import net.autobuilder.core.OptionalParameter;
import net.autobuilder.core.ParamCases;
import net.autobuilder.core.RegularParameter;

public class ClearAccumulatorCases implements ParamCases<Void, CodeBlock.Builder> {

  @Override
  public Void parameter(RegularParameter parameter, CodeBlock.Builder builder) {
    return null;
  }

  @Override
  public Void collectionish(CollectionParameter parameter, CodeBlock.Builder builder) {
    parameter.asBuilderField().ifPresent(builderField ->
        builder.addStatement("this.$N = null", builderField));
    return null;
  }

  @Override
  public Void optionalish(OptionalParameter parameter, CodeBlock.Builder builder) {
    return null;
  }
}
