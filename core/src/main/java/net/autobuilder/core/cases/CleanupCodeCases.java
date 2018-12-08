package net.autobuilder.core.cases;

import com.squareup.javapoet.CodeBlock;
import net.autobuilder.core.Collectionish;
import net.autobuilder.core.Optionalish;
import net.autobuilder.core.ParamCases;
import net.autobuilder.core.Parameter;

public class CleanupCodeCases implements ParamCases<Void, CodeBlock.Builder> {

  @Override
  public Void parameter(Parameter parameter, CodeBlock.Builder builder) {
    if (!parameter.variableElement.asType().getKind().isPrimitive()) {
      builder.addStatement("$L(null)", parameter.setterName);
    }
    return null;
  }

  @Override
  public Void collectionish(Collectionish collectionish, CodeBlock.Builder builder) {
    builder.addStatement("$L(null)",
        collectionish.parameter.setterName);
    return null;
  }

  @Override
  public Void optionalish(Optionalish optionalish, CodeBlock.Builder builder) {
    builder.addStatement("$L(($T) null)",
        optionalish.parameter.setterName,
        optionalish.parameter.type());
    return null;
  }
}
