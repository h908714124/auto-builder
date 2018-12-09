package net.autobuilder.core.cases;

import com.squareup.javapoet.CodeBlock;
import net.autobuilder.core.Collectionish;
import net.autobuilder.core.Optionalish;
import net.autobuilder.core.ParamCases;
import net.autobuilder.core.Parameter;

public class ClearAccumulatorCases implements ParamCases<Void, CodeBlock.Builder> {

  @Override
  public Void parameter(Parameter parameter, CodeBlock.Builder builder) {
    return null;
  }

  @Override
  public Void collectionish(Collectionish collectionish, CodeBlock.Builder builder) {
    collectionish.asBuilderField().ifPresent(builderField ->
        builder.addStatement("this.$N = null", builderField));
    return null;
  }

  @Override
  public Void optionalish(Optionalish optionalish, CodeBlock.Builder builder) {
    return null;
  }
}
