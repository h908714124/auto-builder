package net.autobuilder.core.cases;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import net.autobuilder.core.Collectionish;
import net.autobuilder.core.Optionalish;
import net.autobuilder.core.ParamCases;
import net.autobuilder.core.Parameter;

public class SetterAssignmentCases implements ParamCases<CodeBlock, Void> {

  @Override
  public CodeBlock parameter(Parameter parameter, Void _null) {
    FieldSpec field = parameter.asField();
    ParameterSpec p = parameter.asSetterParameter();
    return CodeBlock.builder()
        .addStatement("this.$N = $N", field, p).build();
  }

  @Override
  public CodeBlock collectionish(Collectionish collectionish, Void _null) {
    return collectionish.setterAssignment();
  }

  @Override
  public CodeBlock optionalish(Optionalish optionalish, Void _null) {
    FieldSpec field = optionalish.parameter.asField();
    ParameterSpec p = optionalish.asSetterParameter();
    return CodeBlock.builder()
        .addStatement("this.$N = $N", field, p).build();
  }
}
