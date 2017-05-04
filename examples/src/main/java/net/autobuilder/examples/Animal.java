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

  static Animal create(String name, int numberOfLegs, boolean isGood,
                       Optional<Optional<String>> maybeMaybe,
                       Optional<String> maybe) {
    return new AutoValue_Animal(name, numberOfLegs, isGood, maybeMaybe, maybe);
  }
}
