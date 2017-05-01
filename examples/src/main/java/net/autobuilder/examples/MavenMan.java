package net.autobuilder.examples;

import com.google.auto.value.AutoValue;
import net.autobuilder.AutoBuilder;

@AutoBuilder
@AutoValue
abstract class MavenMan {

  abstract String name();

  abstract boolean good();

  static MavenMan create(String name, boolean isGood) {
    return new AutoValue_MavenMan(name, isGood);
  }
}
