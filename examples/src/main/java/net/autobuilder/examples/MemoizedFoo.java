package net.autobuilder.examples;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import net.autobuilder.AutoBuilder;

@AutoBuilder
@AutoValue
abstract class MemoizedFoo {

  abstract String barProperty();

  abstract boolean zarProperty();

  @Memoized
  String derivedProperty() {
    return "your " + barProperty() + " is ready";
  }

  @Memoized
  String derivedProperty2() {
    return "your " + zarProperty() + " is ready";
  }
}

