package net.autobuilder.examples;

import com.google.auto.value.AutoValue;
import net.autobuilder.AutoBuilder;

@AutoBuilder
@AutoValue
abstract class GradleMan<S> {

  abstract S getName();
  abstract boolean isGood();

  static <S> GradleMan<S> create(S name, boolean isGood) {
    return new AutoValue_GradleMan<>(name, isGood);
  }
}
