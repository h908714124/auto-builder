package net.autobuilder.core.cases;

import net.autobuilder.core.ParamCases;
import net.autobuilder.core.Collectionish;
import net.autobuilder.core.Optionalish;
import net.autobuilder.core.Parameter;

public class GetParameterCases implements ParamCases<Parameter, Void> {
  @Override
  public Parameter parameter(Parameter parameter, Void _null) {
    return parameter;
  }

  @Override
  public Parameter collectionish(Collectionish collectionish, Void _null) {
    return collectionish.parameter;
  }

  @Override
  public Parameter optionalish(Optionalish optionalish, Void _null) {
    return optionalish.parameter;
  }
}
