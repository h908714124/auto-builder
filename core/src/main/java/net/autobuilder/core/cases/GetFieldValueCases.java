package net.autobuilder.core.cases;

import com.squareup.javapoet.CodeBlock;
import net.autobuilder.core.ParamCases;
import net.autobuilder.core.Collectionish;
import net.autobuilder.core.Optionalish;
import net.autobuilder.core.Parameter;

public class GetFieldValueCases implements ParamCases<CodeBlock, Void> {

  @Override
  public CodeBlock parameter(Parameter parameter, Void _null) {
    return Collectionish.emptyBlock(parameter)
        .orElse(Optionalish.emptyBlock(parameter)
            .orElse(CodeBlock.of("$N.$N",
                parameter.model.builderParameter(),
                parameter.asField())));
  }

  @Override
  public CodeBlock collectionish(Collectionish collectionish, Void _null) {
    return collectionish.getFieldValue();
  }

  @Override
  public CodeBlock optionalish(Optionalish optionalish, Void _null) {
    return optionalish.getFieldValue();
  }
}
