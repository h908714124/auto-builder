package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;

import java.util.Optional;

import static net.autobuilder.core.AutoBuilderProcessor.rawType;

final class RefTrackingBuilder {

  private final FieldSpec inUse;
  private final ClassName perThreadFactoryClass;

  private RefTrackingBuilder(
      FieldSpec inUse,
      ClassName perThreadFactoryClass) {
    this.perThreadFactoryClass = perThreadFactoryClass;
    this.inUse = inUse;
  }

  private static ClassName perThreadFactoryClass(Model model) {
    return rawType(model.generatedClass)
        .nestedClass("PerThreadFactory");
  }

  static Optional<RefTrackingBuilder> create(
      Model model,
      FieldSpec inUse) {
    if (!model.reuse) {
      return Optional.empty();
    }
    ClassName perThreadFactoryClass = perThreadFactoryClass(model);
    return Optional.of(new RefTrackingBuilder(inUse,
        perThreadFactoryClass));
  }
}
