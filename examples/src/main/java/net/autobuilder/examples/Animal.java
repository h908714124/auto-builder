package net.autobuilder.examples;

import com.google.auto.value.AutoValue;
import net.autobuilder.AutoBuilder;

import java.util.Optional;

@AutoBuilder
@AutoValue
abstract class Animal {
 // [...]
  public Animal_Builder toBuilder() {
    return Animal_Builder.builder(this);
  }
}
