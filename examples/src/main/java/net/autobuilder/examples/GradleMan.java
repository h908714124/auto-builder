package net.autobuilder.examples;

import com.google.auto.value.AutoValue;
import net.autobuilder.AutoBuilder;

import java.util.Optional;
import java.util.OptionalInt;

@AutoBuilder
@AutoValue
abstract class GradleMan<S> {

  abstract Optional<S> getName();

  abstract boolean good();
  
  abstract boolean isNice();

  abstract OptionalInt legs();
}
