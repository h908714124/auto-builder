package net.autobuilder.core.cases;

import com.squareup.javapoet.MethodSpec;
import net.autobuilder.core.CollectionParameter;
import net.autobuilder.core.Model;
import net.autobuilder.core.OptionalParameter;
import net.autobuilder.core.ParamCases;
import net.autobuilder.core.RegularParameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gets the additional convenience methods for one parameter.
 */
public class ExtraMethodsCases implements ParamCases<List<MethodSpec>, Model> {

  @Override
  public List<MethodSpec> parameter(RegularParameter parameter, Model model) {
    return Collections.emptyList();
  }

  @Override
  public List<MethodSpec> collectionish(CollectionParameter parameter, Model model) {
    List<MethodSpec> result = new ArrayList<>(2);
    parameter.accumulatorMethod(model).ifPresent(result::add);
    parameter.accumulatorMethodOverload(model).ifPresent(result::add);
    return result;
  }

  @Override
  public List<MethodSpec> optionalish(OptionalParameter parameter, Model model) {
    List<MethodSpec> result = new ArrayList<>(1);
    parameter.convenienceOverloadMethod().ifPresent(result::add);
    return result;
  }
}
