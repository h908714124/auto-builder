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
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
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

  private final FieldSpec inUse;

  private Analyser(Model model) {
    this.model = model;
    this.parameters = model.scan();
    this.initMethod = initMethod(model, parameters);
    String inUseFieldName = uniqueName("inUse", parameters);
    this.inUse = FieldSpec.builder(TypeName.BOOLEAN, inUseFieldName)
        .addModifiers(PRIVATE).build();
    this.staticBuildMethod = staticBuildMethod(
        model, inUse, parameters);
  }

  static Analyser create(Model model) {
    return new Analyser(model);
  }

  TypeSpec analyse() {
    TypeSpec.Builder builder = TypeSpec.classBuilder(rawType(model.generatedClass));
    builder.addMethod(initMethod);
    builder.addMethod(staticBuildMethod);
    if (model.reuse) {
      FieldSpec factoryField = createFactoryField();
      builder.addField(factoryField);
      builder.addField(inUse);
      builder.addType(PerThreadFactory.create(
          model, initMethod, inUse).define());
      builder.addMethod(builderMethodReuse(factoryField));
      builder.addMethod(builderMethodWithParamReuse(factoryField));
    } else {
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
    return builder.addModifiers(FINAL)
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(PRIVATE).build())
        .addJavadoc(generatedInfo())
        .build();
  }

  private FieldSpec createFactoryField() {
    ClassName perThreadFactoryClass = model.perThreadFactoryClass();
    ParameterizedTypeName factoryFieldType = ParameterizedTypeName.get(ClassName.get(ThreadLocal.class), perThreadFactoryClass);
    String factoryFieldName = uniqueName("FACTORY", parameters);
    return FieldSpec.builder(factoryFieldType, factoryFieldName)
        .addModifiers(PRIVATE, STATIC, FINAL)
        .initializer("$T.withInitial($T::new)", ThreadLocal.class, perThreadFactoryClass).build();
  }

  private static String uniqueName(String suffix, List<ParaParameter> parameters) {
    while (isFactoryFieldNameCollision(suffix, parameters)) {
      suffix = "_" + suffix;
    }
    return suffix;
  }

  private static boolean isFactoryFieldNameCollision(
      String factoryFieldName,
      List<ParaParameter> parameters) {
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
        .addStatement("return new $T()", model.generatedClass)
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec builderMethodWithParam() {
    ParameterSpec builder = model.builderParameter();
    ParameterSpec input = ParameterSpec.builder(TypeName.get(model.sourceClass().asType()), "input").build();
    CodeBlock.Builder block = CodeBlock.builder()
        .addStatement("$T $N = new $T()", builder.type, builder, model.generatedClass)
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
      FieldSpec inUse,
      List<ParaParameter> parameters) {
    ParameterSpec result = ParameterSpec.builder(TypeName.get(model.sourceClass().asType()), "result")
        .build();
    List<CodeBlock> invocation = parameters.stream()
        .map(ParaParameter::getFieldValue)
        .collect(Collectors.toList());
    CodeBlock.Builder cleanup = CodeBlock.builder();
    if (model.reuse) {
      parameters.forEach(parameter -> parameter.cleanupCode(cleanup));
    }
    MethodSpec.Builder spec = MethodSpec.methodBuilder("build");
    spec.addCode("$T $N = new $T(\n    ", TypeName.get(model.sourceClass().asType()), result, model.avType)
        .addCode(invocation.stream().collect(joinCodeBlocks(",\n    ")))
        .addCode(");\n")
        .addCode(cleanup.build());
    if (model.reuse) {
      spec.addStatement("$N = $L", inUse, false);
    }
    return spec.addStatement("return $N", result)
        .returns(TypeName.get(model.sourceClass().asType()))
        .addModifiers(model.maybePublic())
        .build();
  }

  private CodeBlock generatedInfo() {
    return CodeBlock.builder().add("Generated by $L\n\n" +
            "@see <a href=\"https://github.com/h908714124/auto-builder\">auto-builder on github</a>\n",
        AutoBuilderProcessor.class.getName()).build();
  }
}
