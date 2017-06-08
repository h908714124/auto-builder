package net.autobuilder.core;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Generated;

import static java.util.Arrays.asList;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.autobuilder.core.AutoBuilderProcessor.rawType;
import static net.autobuilder.core.ParaParameter.ADD_ACCUMULATOR_FIELD;
import static net.autobuilder.core.ParaParameter.ADD_ACCUMULATOR_METHOD;
import static net.autobuilder.core.ParaParameter.ADD_ACCUMULATOR_OVERLOAD;
import static net.autobuilder.core.ParaParameter.ADD_OPTIONALISH_OVERLOAD;
import static net.autobuilder.core.ParaParameter.AS_SETTER_PARAMETER;
import static net.autobuilder.core.ParaParameter.CLEANUP_CODE;
import static net.autobuilder.core.ParaParameter.CLEAR_ACCUMULATOR;
import static net.autobuilder.core.ParaParameter.GET_FIELD_VALUE;
import static net.autobuilder.core.ParaParameter.GET_PARAMETER;
import static net.autobuilder.core.ParaParameter.SETTER_ASSIGNMENT;
import static net.autobuilder.core.Util.joinCodeBlocks;

final class Analyser {

  private final Model model;
  private final List<ParaParameter> parameters;
  private final MethodSpec initMethod;
  private final MethodSpec staticBuildMethod;
  private final Optional<RefTrackingBuilder> optionalRefTrackingBuilder;

  private Analyser(Model model) {
    this.model = model;
    this.parameters = model.scan();
    this.initMethod = initMethod(model, parameters);
    this.staticBuildMethod = staticBuildMethod(model, parameters);
    this.optionalRefTrackingBuilder =
        RefTrackingBuilder.create(model, staticBuildMethod);
  }

  static Analyser create(Model model) {
    return new Analyser(model);
  }

  TypeSpec analyse() {
    TypeSpec.Builder builder = TypeSpec.classBuilder(rawType(model.generatedClass));
    builder.addTypeVariables(model.typevars());
    builder.addMethod(builderMethod());
    builder.addMethod(builderMethodWithParam());
    builder.addMethod(perThreadFactoryMethod());
    builder.addMethod(initMethod);
    builder.addMethod(staticBuildMethod);
    builder.addMethod(abstractBuildMethod());
    builder.addType(SimpleBuilder.create(model, staticBuildMethod).define());
    OptionalConsumer.of(optionalRefTrackingBuilder)
        .ifPresent(refTrackingBuilder -> {
          builder.addType(refTrackingBuilder.define());
          builder.addType(PerThreadFactory.create(model, initMethod, refTrackingBuilder)
              .define());
        })
        .otherwise(() -> {
          builder.addType(PerThreadFactory.createStub(model));
        });
    for (ParaParameter parameter : parameters) {
      builder.addField(GET_PARAMETER.apply(parameter).asField());
      builder.addMethod(setterMethod(parameter));
      ADD_OPTIONALISH_OVERLOAD.accept(parameter, builder);
      ADD_ACCUMULATOR_FIELD.accept(parameter, builder);
      ADD_ACCUMULATOR_METHOD.accept(parameter, builder);
      ADD_ACCUMULATOR_OVERLOAD.accept(parameter, builder);
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

  private MethodSpec perThreadFactoryMethod() {
    MethodSpec.Builder builder = MethodSpec.methodBuilder("perThreadFactory")
        .returns(RefTrackingBuilder.perThreadFactoryClass(model))
        .addModifiers(STATIC);
    OptionalConsumer.of(optionalRefTrackingBuilder)
        .ifPresent(refTrackingBuilder -> builder.addStatement("return new $T()",
            refTrackingBuilder.perThreadFactoryClass))
        .otherwise(() -> builder.addStatement("throw new $T(\n$S)",
            UnsupportedOperationException.class, model.cacheWarning())
            .addModifiers(PRIVATE));
    return builder.build();
  }

  private static MethodSpec initMethod(
      Model model, List<ParaParameter> parameters) {
    ParameterSpec builder = ParameterSpec.builder(model.generatedClass, "builder").build();
    ParameterSpec input = ParameterSpec.builder(model.sourceClass, "input").build();
    CodeBlock.Builder block = CodeBlock.builder();
    for (ParaParameter parameter : parameters) {
      block.addStatement("$N.$N = $N.$L()",
          builder, GET_PARAMETER.apply(parameter).setterName, input,
          GET_PARAMETER.apply(parameter).getterName);
    }
    return MethodSpec.methodBuilder("init")
        .addCode(block.build())
        .addParameters(asList(builder, input))
        .addModifiers(PRIVATE, STATIC)
        .addTypeVariables(model.typevars())
        .build();
  }

  private MethodSpec setterMethod(ParaParameter parameter) {
    ParameterSpec p = AS_SETTER_PARAMETER.apply(parameter);
    CodeBlock.Builder block = CodeBlock.builder();
    block.add(SETTER_ASSIGNMENT.apply(parameter));
    CLEAR_ACCUMULATOR.accept(parameter, block);
    block.addStatement("return this");
    return MethodSpec.methodBuilder(
        GET_PARAMETER.apply(parameter).setterName)
        .addCode(block.build())
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

  private static MethodSpec staticBuildMethod(Model model, List<ParaParameter> parameters) {
    ParameterSpec builder = ParameterSpec.builder(model.generatedClass, "builder")
        .build();
    ParameterSpec result = ParameterSpec.builder(model.sourceClass, "result")
        .build();
    List<CodeBlock> invocation = new ArrayList<>(parameters.size());
    for (ParaParameter parameter : parameters) {
      invocation.add(GET_FIELD_VALUE.apply(parameter, builder));
    }
    CodeBlock.Builder cleanup = CodeBlock.builder();
    for (ParaParameter parameter : parameters) {
      CLEANUP_CODE.apply(parameter, builder).ifPresent(cleanup::add);
    }
    return MethodSpec.methodBuilder("build")
        .addCode("$T $N = new $T(\n    ", model.sourceClass, result, model.avType)
        .addCode(invocation.stream().collect(joinCodeBlocks(",\n    ")))
        .addCode(");\n")
        .addCode(cleanup.build())
        .addStatement("return $N", result)
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
