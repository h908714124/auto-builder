package net.autobuilder.examples;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.autobuilder.AutoBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;

@AutoBuilder
@AutoValue
abstract class Bird {
  abstract ImmutableList<String> feathers();
  abstract ImmutableSet<String> feet();
  abstract ImmutableMap<String, String> eyes();

  abstract List<String> beak();
  abstract Set<String> wings();
  abstract Map<String, String> tail();

  @AutoBuilder
  @AutoValue
  static abstract class Nest {
    abstract String name();

    abstract int weightInGrams();
  }


}
