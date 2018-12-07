package net.autobuilder.core.cases;

import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.autobuilder.core.Collectionish;
import net.autobuilder.core.Optionalish;
import net.autobuilder.core.ParamCases;
import net.autobuilder.core.Parameter;

public class AsSetterParameterCases implements ParamCases<ParameterSpec, Void> {
  @Override
  public ParameterSpec parameter(Parameter parameter, Void _null) {
    return ParameterSpec.builder(TypeName.get(parameter.type), parameter.setterName).build();
  }

  @Override
  public ParameterSpec collectionish(Collectionish collectionish, Void _null) {
    return collectionish.asSetterParameter();
  }

  @Override
  public ParameterSpec optionalish(Optionalish optionalish, Void _null) {
    return ParameterSpec.builder(TypeName.get(optionalish.parameter.type),
        optionalish.parameter.setterName).build();
  }
}
