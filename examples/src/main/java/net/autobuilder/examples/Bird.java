package net.autobuilder.examples;

import com.google.auto.value.AutoValue;
import net.autobuilder.AutoBuilder;

@AutoBuilder
@AutoValue
abstract class Bird {
  abstract String name();

  abstract int numberOfLegs();

  @AutoBuilder
  @AutoValue
  static abstract class Nest {
    abstract String name();

    abstract int weightInGrams();
  }


}
