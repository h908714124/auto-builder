package net.autobuilder.core;

public interface ParamCases<R, P> {

  R parameter(Parameter parameter, P p);

  R collectionish(Collectionish collectionish, P p);

  R optionalish(Optionalish optionalish, P p);
}
