package net.autobuilder.core.cases;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import net.autobuilder.core.CollectionParameter;
import net.autobuilder.core.OptionalParameter;
import net.autobuilder.core.ParamCases;
import net.autobuilder.core.RegularParameter;

public class SetterAssignmentCases implements ParamCases<CodeBlock, Void> {

  @Override
  public CodeBlock parameter(RegularParameter parameter, Void _null) {
    FieldSpec field = parameter.asField();
    ParameterSpec p = parameter.asSetterParameter();
    return CodeBlock.builder()
        .addStatement("this.$N = $N", field, p).build();
  }

  @Override
  public CodeBlock collectionish(CollectionParameter parameter, Void _null) {
    return parameter.setterAssignment();
  }

  @Override
  public CodeBlock optionalish(OptionalParameter parameter, Void _null) {
    FieldSpec field = parameter.parameter.asField();
    ParameterSpec p = parameter.asSetterParameter();
    return CodeBlock.builder()
        .addStatement("this.$N = $N", field, p).build();
  }
}
