package net.autobuilder.examples;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.autobuilder.AutoBuilder;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@AutoBuilder
@AutoValue
abstract class Bird {

  private static final ThreadLocal<Bird_Builder.PerThreadFactory> FACTORY =
      ThreadLocal.withInitial(Bird_Builder::perThreadFactory);

  abstract ImmutableList<Date> feathers();
  abstract ImmutableSet<String> feet();
  abstract ImmutableMap<String, String> eyes();

  abstract List<Date> beak();
  abstract Set<String> wings();
  abstract Map<Date, String> tail();

  final Bird_Builder toBuilder() {
    return FACTORY.get().builder(this);
  }

  @AutoBuilder
  @AutoValue
  static abstract class Nest {
    abstract ImmutableList<? extends Iterable<? extends String>> feathers();
    abstract ImmutableList<String> sticks();
    abstract String addToSticks();
  }
}
