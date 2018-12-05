package net.autobuilder.core;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;

final class PerThreadFactory {

  private final Model model;
  private final MethodSpec initMethod;
  private final FieldSpec builder;
  private final RefTrackingBuilder refTrackingBuilder;

  private PerThreadFactory(Model model, MethodSpec initMethod, RefTrackingBuilder refTrackingBuilder) {
    this.model = model;
    this.initMethod = initMethod;
    this.builder = FieldSpec.builder(refTrackingBuilder.refTrackingBuilderClass, "builder")
        .build();
    this.refTrackingBuilder = refTrackingBuilder;
  }

  static PerThreadFactory create(Model model, MethodSpec initMethod,
                                 RefTrackingBuilder refTrackingBuilder) {
    return new PerThreadFactory(model, initMethod, refTrackingBuilder);
  }

  TypeSpec define() {
    return TypeSpec.classBuilder(refTrackingBuilder.perThreadFactoryClass)
        .addField(builder)
        .addMethod(builderMethod())
        .addMethod(builderMethodWithoutParam())
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build();
  }

  private MethodSpec builderMethod() {
    ParameterSpec input = ParameterSpec.builder(TypeName.get(model.sourceClass().asType()), "input").build();
    CodeBlock.Builder block = CodeBlock.builder()
        .beginControlFlow("if (this.$N == null || this.$N.$N)", builder, builder, refTrackingBuilder.inUse)
        .addStatement("this.$N = new $T()", builder, refTrackingBuilder.refTrackingBuilderClass)
        .endControlFlow()
        .addStatement("$T.$N(this.$N, $N)", model.generatedClass, initMethod, builder, input)
        .addStatement("this.$N.$N = $L", builder, refTrackingBuilder.inUse, true)
        .addStatement("return $N", builder);
    return MethodSpec.methodBuilder("builder")
        .addParameter(input)
        .addCode(block.build())
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec builderMethodWithoutParam() {
    CodeBlock.Builder block = CodeBlock.builder()
        .beginControlFlow("if (this.$N == null || this.$N.$N)", builder, builder, refTrackingBuilder.inUse)
        .addStatement("this.$N = new $T()", builder, refTrackingBuilder.refTrackingBuilderClass)
        .endControlFlow()
        .addStatement("this.$N.$N = $L", builder, refTrackingBuilder.inUse, true)
        .addStatement("return $N", builder);
    return MethodSpec.methodBuilder("builder")
        .addCode(block.build())
        .returns(model.generatedClass)
        .build();
  }
}
