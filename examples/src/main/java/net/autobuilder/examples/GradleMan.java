package net.autobuilder.examples;

import com.google.auto.value.AutoValue;
import net.autobuilder.AutoBuilder;

@AutoBuilder
@AutoValue
abstract class GradleMan<S> {

  abstract S getName();
  abstract boolean isGood();
}
