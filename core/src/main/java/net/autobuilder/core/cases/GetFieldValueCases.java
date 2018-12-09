package net.autobuilder.core.cases;

import com.squareup.javapoet.CodeBlock;
import net.autobuilder.core.CollectionParameter;
import net.autobuilder.core.OptionalParameter;
import net.autobuilder.core.ParamCases;
import net.autobuilder.core.RegularParameter;

public class GetFieldValueCases implements ParamCases<CodeBlock, Void> {

  @Override
  public CodeBlock parameter(RegularParameter parameter, Void _null) {
    return CodeBlock.of("$N", parameter.asField());
  }

  @Override
  public CodeBlock collectionish(CollectionParameter parameter, Void _null) {
    return parameter.getFieldValue();
  }

  @Override
  public CodeBlock optionalish(OptionalParameter parameter, Void _null) {
    return parameter.getFieldValue();
  }
}
