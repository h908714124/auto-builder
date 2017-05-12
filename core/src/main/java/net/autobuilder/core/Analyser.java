package net.autobuilder.core;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.Generated;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.autobuilder.core.AutoBuilderProcessor.rawType;

final class Analyser {

  private final Model model;
  private final MethodSpec initMethod;
  private final MethodSpec staticBuildMethod;
  private final RefTrackingBuilder optionalRefTrackingBuilder;

  private Analyser(Model model) {
    this.model = model;
    this.initMethod = initMethod(model);
    this.staticBuildMethod = staticBuildMethod(model);
    this.optionalRefTrackingBuilder = RefTrackingBuilder.create(model, staticBuildMethod);
  }

  static Analyser create(Model model) {
    return new Analyser(model);
  }

  TypeSpec analyse() {
    TypeSpec.Builder builder = TypeSpec.classBuilder(rawType(model.generatedClass));
    builder.addTypeVariables(model.typevars());
    builder.addMethod(builderMethod());
    builder.addMethod(builderMethodWithParam());
    builder.addMethod(perThreadFactoryMethod(optionalRefTrackingBuilder));
    builder.addMethod(initMethod);
    builder.addMethod(staticBuildMethod);
    builder.addMethod(abstractBuildMethod());
    builder.addType(SimpleBuilder.create(model, staticBuildMethod).define());
    if (optionalRefTrackingBuilder != null) {
      RefTrackingBuilder refTrackingBuilder = requireNonNull(optionalRefTrackingBuilder);
      builder.addType(refTrackingBuilder.define());
      builder.addType(PerThreadFactory.create(model, initMethod, refTrackingBuilder).define());
    } else {
      builder.addType(PerThreadFactory.createStub(model));
    }
    for (Parameter parameter : model.parameters) {
      OptionalInfo optionalInfo = OptionalInfo.create(parameter.type);
      FieldSpec field = fieldOf(parameter, optionalInfo);
      ParameterSpec p = parameter.asParameter();
      builder.addField(field);
      builder.addMethod(setterMethod(parameter, field, p));
      if (optionalInfo != null &&
          !optionalInfo.isDoubleOptional()) {
        builder.addMethod(optionalSetterMethod(parameter, optionalInfo, field,
            ParameterSpec.builder(optionalInfo.wrapped,
                parameter.setterName).build()));
      }
    }
    builder.addModifiers(model.maybePublic());
    return builder.addModifiers(ABSTRACT)
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(PRIVATE).build())
        .addAnnotation(AnnotationSpec.builder(Generated.class)
            .addMember("value", "$S", AutoBuilderProcessor.class.getCanonicalName())
            .build())
        .build();
  }

  private MethodSpec perThreadFactoryMethod(RefTrackingBuilder optionalRefTrackingBuilder) {
    MethodSpec.Builder builder = MethodSpec.methodBuilder("perThreadFactory")
        .returns(RefTrackingBuilder.perThreadFactoryClass(model))
        .addModifiers(STATIC);
    if (optionalRefTrackingBuilder != null) {
      RefTrackingBuilder refTrackingBuilder = requireNonNull(optionalRefTrackingBuilder);
      return builder.addStatement("return new $T()",
          refTrackingBuilder.perThreadFactoryClass)
          .build();
    } else {
      return builder.addStatement("throw new $T(\n$S)",
          UnsupportedOperationException.class, model.cacheWarning())
          .addModifiers(PRIVATE)
          .build();
    }
  }

  private static FieldSpec fieldOf(Parameter parameter, OptionalInfo optionalInfo) {
    FieldSpec.Builder fieldBuilder = parameter.asField();
    if (optionalInfo != null) {
      fieldBuilder.initializer("$T.empty()", optionalInfo.wrapper);
    }
    return fieldBuilder.build();
  }

  private static MethodSpec initMethod(Model model) {
    ParameterSpec builder = ParameterSpec.builder(model.generatedClass, "builder").build();
    ParameterSpec input = ParameterSpec.builder(model.sourceClass, "input").build();
    CodeBlock.Builder block = CodeBlock.builder();
    block.beginControlFlow("if ($N == null)", input)
        .addStatement("throw new $T($S)",
            NullPointerException.class, "Null " + input.name)
        .endControlFlow();
    for (Parameter parameter : model.parameters) {
      block.addStatement("$N.$N = $N.$L()", builder, parameter.setterName, input,
          parameter.getterName);
    }
    return MethodSpec.methodBuilder("init")
        .addCode(block.build())
        .addParameters(Arrays.asList(builder, input))
        .addModifiers(PRIVATE, STATIC)
        .addTypeVariables(model.typevars())
        .build();
  }

  private MethodSpec optionalSetterMethod(Parameter parameter, OptionalInfo optionalInfo, FieldSpec f, ParameterSpec p) {
    return MethodSpec.methodBuilder(
        parameter.setterName)
        .addStatement("this.$N = $T.of($N)", f, optionalInfo.wrapper, p)
        .addStatement("return this")
        .addParameter(p)
        .addModifiers(FINAL)
        .addModifiers(model.maybePublic())
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec setterMethod(Parameter parameter, FieldSpec f, ParameterSpec p) {
    return MethodSpec.methodBuilder(
        parameter.setterName)
        .addStatement("this.$N = $N", f, p)
        .addStatement("return this")
        .addParameter(p)
        .addModifiers(FINAL)
        .addModifiers(model.maybePublic())
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec builderMethod() {
    return MethodSpec.methodBuilder("builder")
        .addModifiers(STATIC)
        .addTypeVariables(model.typevars())
        .addStatement("return new $T()", model.simpleBuilderClass)
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec builderMethodWithParam() {
    ParameterSpec builder = ParameterSpec.builder(model.generatedClass, "builder").build();
    ParameterSpec input = ParameterSpec.builder(model.sourceClass, "input").build();
    CodeBlock.Builder block = CodeBlock.builder()
        .addStatement("$T $N = new $T()", builder.type, builder, model.simpleBuilderClass)
        .addStatement("$N($N, $N)", initMethod, builder, input)
        .addStatement("return $N", builder);
    return MethodSpec.methodBuilder("builder")
        .addCode(block.build())
        .addParameter(input)
        .addModifiers(STATIC)
        .addTypeVariables(model.typevars())
        .returns(model.generatedClass)
        .build();
  }

  private static MethodSpec staticBuildMethod(Model model) {
    ParameterSpec builder = ParameterSpec.builder(model.generatedClass, "builder").build();
    CodeBlock.Builder block = CodeBlock.builder();
    for (int i = 0; i < model.parameters.size(); i++) {
      Parameter parameter = model.parameters.get(i);
      FieldSpec f = parameter.asField().build();
      if (i > 0) {
        block.add(",");
      }
      block.add("\n    $N.$N", builder, f);
    }
    return MethodSpec.methodBuilder("build")
        .addCode("return new $T(", model.avType)
        .addCode(block.build())
        .addCode(");\n")
        .addTypeVariables(model.typevars())
        .returns(model.sourceClass)
        .addParameter(builder)
        .addModifiers(PRIVATE, STATIC)
        .build();
  }

  private MethodSpec abstractBuildMethod() {
    return MethodSpec.methodBuilder("build")
        .returns(model.sourceClass)
        .addModifiers(ABSTRACT)
        .addModifiers(model.maybePublic())
        .build();
  }
}
