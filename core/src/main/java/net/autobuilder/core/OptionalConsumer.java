package net.autobuilder.core;

import java.util.Optional;
import java.util.function.Consumer;

final class OptionalConsumer<T> {

  private final Optional<T> optional;

  private OptionalConsumer(Optional<T> optional) {
    this.optional = optional;
  }

  static abstract class Otherwise {
    abstract void otherwise(Runnable runnable);
  }

  private static final Otherwise NO_OP = new Otherwise() {
    @Override
    void otherwise(Runnable runnable) {
    }
  };

  private static final Otherwise OP = new Otherwise() {
    @Override
    void otherwise(Runnable runnable) {
      runnable.run();
    }
  };

  static <T> OptionalConsumer<T> of(Optional<T> optional) {
    return new OptionalConsumer<>(optional);
  }

  Otherwise ifPresent(Consumer<T> consumer) {
    optional.ifPresent(consumer);
    return optional.isPresent() ? NO_OP : OP;
  }
}
