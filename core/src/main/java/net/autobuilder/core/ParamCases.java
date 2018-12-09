package net.autobuilder.core;

public interface ParamCases<R, P> {

  R parameter(RegularParameter parameter, P p);

  R collectionish(CollectionParameter parameter, P p);

  R optionalish(OptionalParameter parameter, P p);
}
