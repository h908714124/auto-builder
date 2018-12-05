package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
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
    this.staticBuildMethod = staticBuildMethod(model, parameters, model.optionalRefTrackingBuilderClass.isPresent());
    this.optionalRefTrackingBuilder =
        RefTrackingBuilder.create(model, staticBuildMethod);
  }

  static Analyser create(Model model) {
    return new Analyser(model);
  }

  TypeSpec analyse() {
    TypeSpec.Builder builder = TypeSpec.classBuilder(rawType(model.generatedClass));
    builder.addMethod(initMethod);
    builder.addMethod(staticBuildMethod);
    builder.addMethod(abstractBuildMethod());
    if (optionalRefTrackingBuilder.isPresent()) {
      optionalRefTrackingBuilder
          .ifPresent(refTrackingBuilder -> {
            FieldSpec factoryField = createFactoryField(refTrackingBuilder);
            builder.addField(factoryField);
            builder.addType(refTrackingBuilder.define());
            builder.addType(PerThreadFactory.create(model, initMethod, refTrackingBuilder)
                .define());
            builder.addMethod(builderMethodReuse(factoryField));
            builder.addMethod(builderMethodWithParamReuse(factoryField));
          });
    } else {
      builder.addType(SimpleBuilder.create(model, staticBuildMethod).define());
      builder.addMethod(builderMethod());
      builder.addMethod(builderMethodWithParam());
    }
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

  private FieldSpec createFactoryField(RefTrackingBuilder refTrackingBuilder) {
    ParameterizedTypeName factoryFieldType = ParameterizedTypeName.get(ClassName.get(ThreadLocal.class), refTrackingBuilder.perThreadFactoryClass);
    String factoryFieldName = "FACTORY";
    while (isFactoryFieldNameCollision(factoryFieldName)) {
      factoryFieldName = "_" + factoryFieldName;
    }
    return FieldSpec.builder(factoryFieldType, factoryFieldName)
        .addModifiers(PRIVATE, STATIC, FINAL)
        .initializer("$T.withInitial($T::new)", ThreadLocal.class, refTrackingBuilder.perThreadFactoryClass).build();
  }


  private boolean isFactoryFieldNameCollision(String factoryFieldName) {
    for (ParaParameter parameter : parameters) {
      if (parameter.getParameter().asField().name.equals(factoryFieldName)) {
        return true;
      }
    }
    return false;
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
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec builderMethodReuse(FieldSpec factoryField) {
    return MethodSpec.methodBuilder("builder")
        .addModifiers(STATIC)
        .addStatement("return $N.get().builder()", factoryField)
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec builderMethodWithParamReuse(FieldSpec factoryField) {
    ParameterSpec input = ParameterSpec.builder(TypeName.get(model.sourceClass().asType()), "input").build();
    return MethodSpec.methodBuilder("builder")
        .addModifiers(STATIC)
        .addStatement("return $N.get().builder($N)", factoryField, input)
        .addParameter(input)
        .returns(model.generatedClass)
        .build();
  }

  private static MethodSpec staticBuildMethod(
      Model model,
      List<ParaParameter> parameters,
      boolean addCleanupCode) {
    ParameterSpec builder = model.builderParameter();
    ParameterSpec result = ParameterSpec.builder(TypeName.get(model.sourceClass().asType()), "result")
        .build();
    List<CodeBlock> invocation = parameters.stream()
        .map(ParaParameter::getFieldValue)
        .collect(Collectors.toList());
    CodeBlock.Builder cleanup = CodeBlock.builder();
    if (addCleanupCode) {
      parameters.forEach(parameter -> parameter.cleanupCode(cleanup));
    }
    return MethodSpec.methodBuilder("build")
        .addCode("$T $N = new $T(\n    ", TypeName.get(model.sourceClass().asType()), result, model.avType)
        .addCode(invocation.stream().collect(joinCodeBlocks(",\n    ")))
        .addCode(");\n")
        .addCode(cleanup.build())
        .addStatement("return $N", result)
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
