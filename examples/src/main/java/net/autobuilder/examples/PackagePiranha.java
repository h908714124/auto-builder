package net.autobuilder.examples;

import com.google.auto.value.AutoValue;
import net.autobuilder.AutoBuilder;

@AutoValue
@AutoBuilder
abstract class PackagePiranha {
  abstract String foo();
}
