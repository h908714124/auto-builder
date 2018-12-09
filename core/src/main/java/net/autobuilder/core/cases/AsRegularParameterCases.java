package net.autobuilder.core.cases;

import net.autobuilder.core.CollectionParameter;
import net.autobuilder.core.OptionalParameter;
import net.autobuilder.core.ParamCases;
import net.autobuilder.core.RegularParameter;

public class AsRegularParameterCases implements ParamCases<RegularParameter, Void> {

  @Override
  public RegularParameter parameter(RegularParameter parameter, Void _null) {
    return parameter;
  }

  @Override
  public RegularParameter collectionish(CollectionParameter parameter, Void _null) {
    return parameter.parameter;
  }

  @Override
  public RegularParameter optionalish(OptionalParameter parameter, Void _null) {
    return parameter.parameter;
  }
}
