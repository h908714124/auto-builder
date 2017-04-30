package net.autobuilder.core;

import com.squareup.javapoet.TypeSpec;

final class Analyser {

  private final Processor.Model model;

  private Analyser(Processor.Model model) {
    this.model = model;
  }

  static Analyser create(Processor.Model model) {
    return new Analyser(model);
  }

  TypeSpec analyse() {
    return null;
  }
}
