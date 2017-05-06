package net.autobuilder.examples;

import com.google.auto.value.AutoValue;
import net.autobuilder.AutoBuilder;

import java.util.Optional;

@AutoBuilder
@AutoValue
abstract class Animal {

  private static final ThreadLocal<Animal_Builder.PerThreadFactory> FACTORY =
      ThreadLocal.withInitial(Animal_Builder::perThreadFactory);

  abstract String getName();

  abstract int getNumberOfLegs();

  abstract boolean isGood();

  abstract Optional<Optional<String>> maybeMaybe();

  abstract Optional<String> maybe();

  final Animal_Builder toBuilder() {
    return FACTORY.get().builder(this);
  }
}
