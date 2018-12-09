package net.autobuilder.core.cases;

import com.squareup.javapoet.CodeBlock;
import net.autobuilder.core.CollectionParameter;
import net.autobuilder.core.OptionalParameter;
import net.autobuilder.core.ParamCases;
import net.autobuilder.core.RegularParameter;

public class CleanupCodeCases implements ParamCases<Void, CodeBlock.Builder> {

  @Override
  public Void parameter(RegularParameter parameter, CodeBlock.Builder builder) {
    if (!parameter.variableElement.asType().getKind().isPrimitive()) {
      builder.addStatement("$L(null)", parameter.setterName);
    }
    return null;
  }

  @Override
  public Void collectionish(CollectionParameter parameter, CodeBlock.Builder builder) {
    builder.addStatement("$L(null)",
        parameter.parameter.setterName);
    return null;
  }

  @Override
  public Void optionalish(OptionalParameter parameter, CodeBlock.Builder builder) {
    builder.addStatement("$L(($T) null)",
        parameter.parameter.setterName,
        parameter.parameter.type());
    return null;
  }
}
