package net.autobuilder.core.cases;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import net.autobuilder.core.CollectionParameter;
import net.autobuilder.core.OptionalParameter;
import net.autobuilder.core.ParamCases;
import net.autobuilder.core.RegularParameter;

/**
 * Gets the builder field value for one parameter,
 * while replacing null for lists and optionals.
 */
public class ExtractCases implements ParamCases<CodeBlock, Void> {

  @Override
  public CodeBlock parameter(RegularParameter parameter, Void _null) {
    return CodeBlock.of("$N", parameter.asField());
  }

  @Override
  public CodeBlock collectionish(CollectionParameter parameter, Void _null) {
    CodeBlock.Builder code = CodeBlock.builder();
    FieldSpec field = parameter.parameter.asField();
    parameter.asBuilderField().ifPresent(builderField ->
        code.add("$N != null ? $L : ",
            builderField, parameter.base.buildBlock(builderField)));
    code.add("$N != null ? $N : $L",
        field, field, parameter.base.emptyBlock());
    return code.build();
  }

  @Override
  public CodeBlock optionalish(OptionalParameter parameter, Void _null) {
    FieldSpec field = parameter.parameter.asField();
    return CodeBlock.of("$N != null ? $N : $T.empty()",
        field, field, parameter.wrapper);
  }
}
