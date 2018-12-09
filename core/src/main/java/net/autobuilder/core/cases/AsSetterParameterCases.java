package net.autobuilder.core.cases;

import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.autobuilder.core.CollectionParameter;
import net.autobuilder.core.OptionalParameter;
import net.autobuilder.core.ParamCases;
import net.autobuilder.core.RegularParameter;

/**
 *
 */
public class AsSetterParameterCases implements ParamCases<ParameterSpec, Void> {

  @Override
  public ParameterSpec parameter(RegularParameter parameter, Void _null) {
    return ParameterSpec.builder(TypeName.get(parameter.type()), parameter.setterName).build();
  }

  @Override
  public ParameterSpec collectionish(CollectionParameter parameter, Void _null) {
    return parameter.base.setterParameter(parameter.parameter);
  }

  @Override
  public ParameterSpec optionalish(OptionalParameter parameter, Void _null) {
    return ParameterSpec.builder(TypeName.get(parameter.parameter.type()),
        parameter.parameter.setterName).build();
  }
}
