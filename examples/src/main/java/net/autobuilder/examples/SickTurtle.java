package net.autobuilder.examples;

import com.google.auto.value.AutoValue;
import net.autobuilder.AutoBuilder;

import java.util.Optional;

@AutoBuilder
@AutoValue
abstract class SickTurtle {
  abstract String name();

  abstract int numberOfLegs();

  @AutoValue.Builder
  abstract static class Builder {
    abstract Builder name(String name);

    abstract Builder numberOfLegs(int numberOfLegs);

    abstract SickTurtle build();

  }
}
