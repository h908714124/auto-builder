package net.autobuilder.examples;

import com.google.auto.value.AutoValue;
import net.autobuilder.AutoBuilder;

@AutoBuilder
@AutoValue
abstract class Animal {
  abstract String getName();

  abstract int getNumberOfLegs();

  static Animal create(String name, int numberOfLegs) {
    return new AutoValue_Animal(name, numberOfLegs);
  }
}
