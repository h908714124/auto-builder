package net.autobuilder.examples;

import com.google.auto.value.AutoValue;
import net.autobuilder.AutoBuilder;

import java.util.Optional;
import java.util.OptionalInt;

@AutoBuilder
@AutoValue
abstract class GradleMan {

  abstract Optional<String> getName();

  abstract String getSnake();

  abstract boolean good();

  abstract boolean isNice();

  abstract OptionalInt legs();
}
