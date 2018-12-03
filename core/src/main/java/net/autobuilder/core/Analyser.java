package net.autobuilder.core;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.autobuilder.core.AutoBuilderProcessor.rawType;
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
        .otherwise(() ->
            builder.addType(PerThreadFactory.createStub(model)));
    for (ParaParameter parameter : parameters) {
      builder.addField(parameter.getParameter().asField());
      builder.addMethod(setterMethod(parameter));
      parameter.addOptionalishOverload(builder);
      parameter.addAccumulatorField(builder);
      parameter.addAccumulatorMethod(builder);
      parameter.addAccumulatorOverload(builder);
    }
    builder.addModifiers(model.maybePublic());
    return builder.addModifiers(ABSTRACT)
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(PRIVATE).build())
        .addJavadoc(generatedInfo())
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
    ParameterSpec input = ParameterSpec.builder(TypeName.get(model.sourceClass().asType()), "input").build();
    CodeBlock.Builder block = CodeBlock.builder();
    for (ParaParameter parameter : parameters) {
      block.addStatement("$N.$N = $N.$L()",
          model.builderParameter(),
          parameter.getParameter().setterName, input,
          parameter.getParameter().getterName);
    }
    return MethodSpec.methodBuilder("init")
        .addCode(block.build())
        .addParameters(asList(model.builderParameter(), input))
        .addModifiers(PRIVATE, STATIC)
        .addTypeVariables(model.typevars())
        .build();
  }

  private MethodSpec setterMethod(ParaParameter parameter) {
    ParameterSpec p = parameter.asSetterParameter();
    CodeBlock.Builder block = CodeBlock.builder();
    block.add(parameter.setterAssignment());
    parameter.clearAccumulator(block);
    block.addStatement("return this");
    return MethodSpec.methodBuilder(
        parameter.getParameter().setterName)
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
    ParameterSpec builder = model.builderParameter();
    ParameterSpec input = ParameterSpec.builder(TypeName.get(model.sourceClass().asType()), "input").build();
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
    ParameterSpec builder = model.builderParameter();
    ParameterSpec result = ParameterSpec.builder(TypeName.get(model.sourceClass().asType()), "result")
        .build();
    List<CodeBlock> invocation = parameters.stream()
        .map(ParaParameter::getFieldValue)
        .collect(Collectors.toList());
    CodeBlock.Builder cleanup = CodeBlock.builder();
    parameters.forEach(parameter -> parameter.cleanupCode(cleanup));
    return MethodSpec.methodBuilder("build")
        .addCode("$T $N = new $T(\n    ", TypeName.get(model.sourceClass().asType()), result, model.avType)
        .addCode(invocation.stream().collect(joinCodeBlocks(",\n    ")))
        .addCode(");\n")
        .addCode(cleanup.build())
        .addStatement("return $N", result)
        .addTypeVariables(model.typevars())
        .returns(TypeName.get(model.sourceClass().asType()))
        .addParameter(builder)
        .addModifiers(PRIVATE, STATIC)
        .build();
  }

  private MethodSpec abstractBuildMethod() {
    return MethodSpec.methodBuilder("build")
        .returns(TypeName.get(model.sourceClass().asType()))
        .addModifiers(ABSTRACT)
        .addModifiers(model.maybePublic())
        .build();
  }

  private CodeBlock generatedInfo() {
    return CodeBlock.builder().add("Generated by $L\n\n" +
            "@see <a href=\"https://github.com/h908714124/auto-builder\">auto-builder on github</a>\n",
        AutoBuilderProcessor.class.getName()).build();
  }
}
