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
  private final FieldSpec inUse;

  private PerThreadFactory(
      Model model,
      MethodSpec initMethod,
      FieldSpec inUse) {
    this.model = model;
    this.initMethod = initMethod;
    this.builder = FieldSpec.builder(model.generatedClass, "builder")
        .build();
    this.inUse = inUse;
  }

  static PerThreadFactory create(
      Model model,
      MethodSpec initMethod,
      FieldSpec inUse) {
    return new PerThreadFactory(model, initMethod, inUse);
  }

  TypeSpec define() {
    return TypeSpec.classBuilder(model.perThreadFactoryClass())
        .addField(builder)
        .addMethod(builderMethod())
        .addMethod(builderMethodWithoutParam())
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build();
  }

  private MethodSpec builderMethod() {
    ParameterSpec input = ParameterSpec.builder(TypeName.get(model.sourceClass().asType()), "input").build();
    CodeBlock.Builder block = CodeBlock.builder()
        .beginControlFlow("if (this.$N == null || this.$N.$N)", builder, builder, inUse)
        .addStatement("this.$N = new $T()", builder, model.generatedClass)
        .endControlFlow()
        .addStatement("$T.$N(this.$N, $N)", model.generatedClass, initMethod, builder, input)
        .addStatement("this.$N.$N = $L", builder, inUse, true)
        .addStatement("return $N", builder);
    return MethodSpec.methodBuilder("builder")
        .addParameter(input)
        .addCode(block.build())
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec builderMethodWithoutParam() {
    CodeBlock.Builder block = CodeBlock.builder()
        .beginControlFlow("if (this.$N == null || this.$N.$N)", builder, builder, inUse)
        .addStatement("this.$N = new $T()", builder, model.generatedClass)
        .endControlFlow()
        .addStatement("this.$N.$N = $L", builder, inUse, true)
        .addStatement("return $N", builder);
    return MethodSpec.methodBuilder("builder")
        .addCode(block.build())
        .returns(model.generatedClass)
        .build();
  }
}
