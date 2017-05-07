package net.autobuilder.examples;

import com.google.auto.value.AutoValue;
import net.autobuilder.AutoBuilder;

@AutoValue
@AutoBuilder
public abstract class PublicPenguin {
  public abstract String foo();
}
