package net.autobuilder.core;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.Generated;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.autobuilder.core.AutoBuilderProcessor.rawType;
import static net.autobuilder.core.Util.joinCodeBlocks;

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
      builder.addField(parameter.asInitializedField());
      builder.addMethod(setterMethod(parameter));
      parameter.optionalish()
          .map(optionalish -> optionalSetterMethod(parameter,
              optionalish))
          .ifPresent(builder::addMethod);
      parameter.collectionish()
          .filter(Collectionish::hasBuilder)
          .ifPresent(collectionish ->
              builder.addField(
                  parameter.asBuilderField()));
      parameter.collectionish()
          .filter(Collectionish::hasBuilder)
          .ifPresent(collectionish -> {
            builder.addMethod(collectorMethod(parameter, collectionish));
            parameter.addAllType().ifPresent(addAllType ->
                builder.addMethod(collectorMethodAddAll(parameter, collectionish, addAllType)));
          });
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

  private MethodSpec collectorMethod(Parameter parameter, Collectionish collectionish) {
    if (collectionish.type == Collectionish.CollectionType.MAP) {
      return putInMethod(parameter, collectionish);
    }
    return addToMethod(parameter, collectionish);
  }

  private MethodSpec collectorMethodAddAll(
      Parameter parameter, Collectionish collectionish, ParameterizedTypeName addAllType) {
    if (collectionish.type == Collectionish.CollectionType.MAP) {
      return putAllInMethod(parameter, collectionish, addAllType);
    }
    return addAllToMethod(parameter, collectionish, addAllType);
  }

  private MethodSpec addToMethod(Parameter parameter, Collectionish collectionish) {
    FieldSpec field = parameter.asField().build();
    FieldSpec builderField = parameter.asBuilderField();
    ParameterizedTypeName builderType = parameter.builderType();
    ParameterSpec key =
        ParameterSpec.builder(builderType.typeArguments.get(0), "value").build();
    CodeBlock.Builder block = CodeBlock.builder();
    block.beginControlFlow("if (this.$N == null)", builderField)
        .addStatement("this.$N = $T.builder()",
            builderField, collectionish.factoryClassName)
        .endControlFlow();
    block.beginControlFlow("if (this.$N != null)", field)
        .add(parameter.addAllBlock(CodeBlock.of("this.$N", field)))
        .addStatement("this.$N = null", field)
        .endControlFlow();
    block.addStatement("this.$N.$L($N)",
        builderField, collectionish.addMethod, key);
    return MethodSpec.methodBuilder(
        parameter.accumulatorName(collectionish))
        .addCode(block.build())
        .addStatement("return this")
        .addParameter(key)
        .addModifiers(FINAL)
        .addModifiers(model.maybePublic())
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec addAllToMethod(
      Parameter parameter, Collectionish collectionish, ParameterizedTypeName addAllType) {
    FieldSpec field = parameter.asField().build();
    FieldSpec builderField = parameter.asBuilderField();
    ParameterSpec values =
        ParameterSpec.builder(addAllType, "values").build();
    CodeBlock.Builder block = CodeBlock.builder();
    block.beginControlFlow("if (this.$N == null)", builderField)
        .addStatement("this.$N = $T.builder()",
            builderField, collectionish.factoryClassName)
        .endControlFlow();
    block.beginControlFlow("if (this.$N != null)", field)
        .add(parameter.addAllBlock(CodeBlock.of("this.$N", field)))
        .addStatement("this.$N = null", field)
        .endControlFlow();
    block.add(parameter.addAllBlock(CodeBlock.of("$N", values)));

    return MethodSpec.methodBuilder(
        parameter.accumulatorName(collectionish))
        .addCode(block.build())
        .addStatement("return this")
        .addParameter(values)
        .addModifiers(FINAL)
        .addModifiers(model.maybePublic())
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec putInMethod(Parameter parameter, Collectionish collectionish) {
    FieldSpec field = parameter.asField().build();
    FieldSpec builderField = parameter.asBuilderField();
    ParameterizedTypeName builderType = parameter.builderType();
    ParameterSpec key =
        ParameterSpec.builder(builderType.typeArguments.get(0), "key").build();
    ParameterSpec value =
        ParameterSpec.builder(builderType.typeArguments.get(1), "value").build();
    CodeBlock.Builder block = CodeBlock.builder();
    block.beginControlFlow("if (this.$N == null)", builderField)
        .addStatement("this.$N = $T.builder()",
            builderField, collectionish.factoryClassName)
        .endControlFlow();
    block.beginControlFlow("if (this.$N != null)", field)
        .add(parameter.addAllBlock(CodeBlock.of("this.$N", field)))
        .addStatement("this.$N = null", field)
        .endControlFlow();
    block.addStatement("this.$N.$L($N, $N)",
        builderField, collectionish.addMethod, key, value);
    return MethodSpec.methodBuilder(
        parameter.accumulatorName(collectionish))
        .addCode(block.build())
        .addStatement("return this")
        .addParameters(asList(key, value))
        .addModifiers(FINAL)
        .addModifiers(model.maybePublic())
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec putAllInMethod(
      Parameter parameter, Collectionish collectionish, ParameterizedTypeName addAllType) {
    FieldSpec field = parameter.asField().build();
    FieldSpec builderField = parameter.asBuilderField();
    ParameterSpec map =
        ParameterSpec.builder(addAllType, "map").build();
    CodeBlock.Builder block = CodeBlock.builder();
    block.beginControlFlow("if (this.$N == null)", builderField)
        .addStatement("this.$N = $T.builder()",
            builderField, collectionish.factoryClassName)
        .endControlFlow();
    block.beginControlFlow("if (this.$N != null)", field)
        .add(parameter.addAllBlock(CodeBlock.of("this.$N", field)))
        .addStatement("this.$N = null", field)
        .endControlFlow();
    block.add(parameter.addAllBlock(CodeBlock.of("$N", map)));

    return MethodSpec.methodBuilder(
        parameter.accumulatorName(collectionish))
        .addCode(block.build())
        .addStatement("return this")
        .addParameter(map)
        .addModifiers(FINAL)
        .addModifiers(model.maybePublic())
        .returns(model.generatedClass)
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

  private static MethodSpec initMethod(Model model) {
    ParameterSpec builder = ParameterSpec.builder(model.generatedClass, "builder").build();
    ParameterSpec input = ParameterSpec.builder(model.sourceClass, "input").build();
    CodeBlock.Builder block = CodeBlock.builder();
    for (Parameter parameter : model.parameters) {
      block.addStatement("$N.$N = $N.$L()", builder, parameter.setterName, input,
          parameter.getterName);
    }
    return MethodSpec.methodBuilder("init")
        .addCode(block.build())
        .addParameters(asList(builder, input))
        .addModifiers(PRIVATE, STATIC)
        .addTypeVariables(model.typevars())
        .build();
  }

  private MethodSpec setterMethod(Parameter parameter) {
    ParameterSpec p = parameter.asParameter();
    CodeBlock.Builder block = CodeBlock.builder();
    block.add(parameter.setterAssignment());
    parameter.collectionish()
        .filter(Collectionish::hasBuilder)
        .ifPresent(collectionish ->
            block.addStatement("this.$N = null",
                parameter.asBuilderField()));
    block.addStatement("return this");
    return MethodSpec.methodBuilder(
        parameter.setterName)
        .addCode(block.build())
        .addParameter(p)
        .addModifiers(FINAL)
        .addModifiers(model.maybePublic())
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec optionalSetterMethod(Parameter parameter, Optionalish optionalish) {
    FieldSpec f = parameter.asField().build();
    ParameterSpec p = ParameterSpec.builder(optionalish.wrapped,
        parameter.setterName).build();
    CodeBlock.Builder block = CodeBlock.builder();
    if (optionalish.isOptional()) {
      block.addStatement("this.$N = $T.ofNullable($N)", f, optionalish.wrapper, p);
    } else {
      block.addStatement("this.$N = $T.of($N)", f, optionalish.wrapper, p);
    }
    return MethodSpec.methodBuilder(
        parameter.setterName)
        .addCode(block.build())
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
    List<CodeBlock> block = new ArrayList<>(model.parameters.size());
    for (int i = 0; i < model.parameters.size(); i++) {
      Parameter parameter = model.parameters.get(i);
      FieldSpec field = parameter.asField().build();
      block.add(parameter.collectionish()
          .filter(Collectionish::hasBuilder)
          .map(collectionish -> {
            FieldSpec builderField = parameter.asBuilderField();
            return CodeBlock.of("$N.$N != null ? $N.$N.build() : $N.$N",
                builder, builderField,
                builder, builderField,
                builder, field);
          })
          .orElse(CodeBlock.of("$N.$N", builder, field)));
    }
    return MethodSpec.methodBuilder("build")
        .addCode("return new $T(\n    ", model.avType)
        .addCode(block.stream().collect(joinCodeBlocks(",\n    ")))
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
