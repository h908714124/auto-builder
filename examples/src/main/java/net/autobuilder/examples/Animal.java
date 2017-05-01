package net.autobuilder.examples;

import com.google.auto.value.AutoValue;
import net.autobuilder.AutoBuilder;

@AutoBuilder
@AutoValue
abstract class Animal {
  abstract String name();

  abstract int numberOfLegs();

  static Animal create(String name, int numberOfLegs) {
    return new AutoValue_Animal(name, numberOfLegs);
  }
}
