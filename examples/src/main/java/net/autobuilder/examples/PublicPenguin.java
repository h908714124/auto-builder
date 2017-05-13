package net.autobuilder.examples;

import com.google.auto.value.AutoValue;
import net.autobuilder.AutoBuilder;

import java.util.Optional;
import java.util.OptionalInt;

@AutoValue
@AutoBuilder
public abstract class PublicPenguin {
  public abstract String foo();
  public abstract Optional<String> friend();
  public abstract OptionalInt bar();
  public final PublicPenguin_Builder toBuilder() {
    return PublicPenguin_Builder.builder(this);
  }
}
