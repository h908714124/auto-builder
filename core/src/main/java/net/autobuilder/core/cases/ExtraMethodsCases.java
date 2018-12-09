package net.autobuilder.core.cases;

import com.squareup.javapoet.MethodSpec;
import net.autobuilder.core.Collectionish;
import net.autobuilder.core.Model;
import net.autobuilder.core.Optionalish;
import net.autobuilder.core.ParamCases;
import net.autobuilder.core.Parameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExtraMethodsCases implements ParamCases<List<MethodSpec>, Model> {

  @Override
  public List<MethodSpec> parameter(Parameter parameter, Model model) {
    return Collections.emptyList();
  }

  @Override
  public List<MethodSpec> collectionish(Collectionish collectionish, Model model) {
    List<MethodSpec> result = new ArrayList<>(2);
    collectionish.accumulatorMethod(model).ifPresent(result::add);
    collectionish.accumulatorMethodOverload(model).ifPresent(result::add);
    return result;
  }

  @Override
  public List<MethodSpec> optionalish(Optionalish optionalish, Model model) {
    List<MethodSpec> result = new ArrayList<>(1);
    optionalish.convenienceOverloadMethod().ifPresent(result::add);
    return result;
  }
}
