package net.autobuilder.core;

abstract class ParaParameter {
  static abstract class Cases<R> {
    abstract R parameter(Parameter parameter);

    abstract R collectionish(Collectionish collectionish);

    abstract R optionalish(Optionalish optionalish);
  }

  abstract <R> R accept(Cases<R> cases);
}
