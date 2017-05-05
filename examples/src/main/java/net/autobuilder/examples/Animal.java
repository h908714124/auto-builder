package net.autobuilder.examples;

import com.google.auto.value.AutoValue;
import net.autobuilder.AutoBuilder;

import java.util.Optional;

@AutoBuilder
@AutoValue
abstract class Animal {
  abstract String getName();

  abstract int getNumberOfLegs();

  abstract boolean isGood();

  abstract Optional<Optional<String>> maybeMaybe();

  abstract Optional<String> maybe();

  public Animal_Builder toBuilder() {
    return Animal_Builder.builder(this);
  }
}
