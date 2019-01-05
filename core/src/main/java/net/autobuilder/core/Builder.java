package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.stream.Collectors;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.autobuilder.core.AutoBuilderProcessor.rawType;
import static net.autobuilder.core.Util.joinCodeBlocks;

/**
 * Generates the *_Builder class.
 */
final class Builder {

  private final Model model;

  private final MethodSpec initMethod;

  private final FieldSpec inUse;

  private Builder(Model model) {
    this.model = model;
    this.initMethod = initMethod(model, model.parameters);
    String inUseFieldName = model.uniqueFieldName("inUse");
    this.inUse = FieldSpec.builder(TypeName.BOOLEAN, inUseFieldName)
        .addModifiers(PRIVATE).build();
  }

  static Builder create(Model model) {
    return new Builder(model);
  }

  TypeSpec define() {
    TypeSpec.Builder spec = TypeSpec.classBuilder(rawType(model.generatedClass));
    spec.addMethod(initMethod);
    spec.addMethod(buildMethod(model, inUse, model.parameters));
    MethodSpec toBuilderMethod;
    if (model.reuse) {
      FieldSpec factoryField = createFactoryField();
      spec.addField(factoryField);
      spec.addField(inUse);
      spec.addType(PerThreadFactory.create(
          model, initMethod, inUse).define());
      spec.addMethod(staticBuilderMethodReuse(factoryField));
      toBuilderMethod = staticToBuilderMethodReuse(factoryField, model.uniqueSetterMethodName("toBuilder", model.sourceElement().asType()));
    } else {
      spec.addMethod(staticBuilderMethod());
      toBuilderMethod = staticToBuilderMethod(model.uniqueSetterMethodName("toBuilder", model.sourceElement().asType()));
    }
    spec.addMethod(toBuilderMethod);
    spec.addMethod(toBuilderAlias(toBuilderMethod.name));
    for (Parameter parameter : model.parameters) {
      spec.addField(parameter.asRegularParameter().asField());
      spec.addMethod(setterMethod(parameter));
      parameter.extraField().ifPresent(spec::addField);
      parameter.getExtraMethods(model).forEach(spec::addMethod);
    }
    return spec.addModifiers(FINAL)
        .addModifiers(model.maybePublic())
        .addMethod(MethodSpec.constructorBuilder().addModifiers(PRIVATE).build())
        .addJavadoc(generatedInfo())
        .build();
  }

  private FieldSpec createFactoryField() {
    ClassName perThreadFactoryClass = model.perThreadFactoryClass();
    ParameterizedTypeName factoryFieldType = ParameterizedTypeName.get(ClassName.get(ThreadLocal.class), perThreadFactoryClass);
    String factoryFieldName = model.uniqueFieldName("FACTORY");
    return FieldSpec.builder(factoryFieldType, factoryFieldName)
        .addModifiers(PRIVATE, STATIC, FINAL)
        .initializer("$T.withInitial($T::new)", ThreadLocal.class, perThreadFactoryClass).build();
  }


  private static MethodSpec initMethod(
      Model model, List<Parameter> parameters) {
    ParameterSpec input = ParameterSpec.builder(TypeName.get(model.sourceElement().asType()), "input").build();
    CodeBlock.Builder block = CodeBlock.builder();
    for (Parameter parameter : parameters) {
      block.addStatement("$N = $N.$L()",
          parameter.asRegularParameter().setterName, input,
          parameter.asRegularParameter().getterName);
    }
    return MethodSpec.methodBuilder(model.uniqueSetterMethodName("init", model.sourceElement().asType()))
        .addCode(block.build())
        .addParameter(input)
        .addModifiers(PRIVATE)
        .build();
  }

  private MethodSpec setterMethod(Parameter parameter) {
    ParameterSpec p = parameter.asSetterParameter();
    CodeBlock.Builder block = CodeBlock.builder();
    block.add(parameter.codeInsideSetter());
    block.addStatement("return this");
    return MethodSpec.methodBuilder(
        parameter.asRegularParameter().setterName)
        .addCode(block.build())
        .addParameter(p)
        .addModifiers(FINAL)
        .addModifiers(model.maybePublic())
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec staticBuilderMethod() {
    return MethodSpec.methodBuilder("builder")
        .addModifiers(STATIC)
        .addModifiers(model.maybePublic())
        .addStatement("return new $T()", model.generatedClass)
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec staticToBuilderMethod(String methodName) {
    ParameterSpec builder = ParameterSpec.builder(model.generatedClass,
        "builder").build();
    ParameterSpec input = ParameterSpec.builder(TypeName.get(model.sourceElement().asType()), "input").build();
    CodeBlock.Builder block = CodeBlock.builder()
        .addStatement("$T $N = new $T()", builder.type, builder, model.generatedClass)
        .addStatement("$N.$N($N)", builder, initMethod, input)
        .addStatement("return $N", builder);
    return MethodSpec.methodBuilder(methodName)
        .addCode(block.build())
        .addParameter(input)
        .addModifiers(STATIC)
        .addModifiers(model.maybePublic())
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec staticBuilderMethodReuse(FieldSpec factoryField) {
    return MethodSpec.methodBuilder("builder")
        .addModifiers(STATIC)
        .addModifiers(model.maybePublic())
        .addStatement("return $N.get().builder()", factoryField)
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec staticToBuilderMethodReuse(
      FieldSpec factoryField, String methodName) {
    ParameterSpec param = ParameterSpec.builder(TypeName.get(model.sourceElement().asType()), "input").build();
    return MethodSpec.methodBuilder(methodName)
        .addModifiers(STATIC)
        .addModifiers(model.maybePublic())
        .addStatement("return $N.get().builder($N)", factoryField, param)
        .addParameter(param)
        .returns(model.generatedClass)
        .build();
  }

  private MethodSpec toBuilderAlias(String toBuilderMethodName) {
    ParameterSpec param = ParameterSpec.builder(TypeName.get(model.sourceElement().asType()), "input").build();
    return MethodSpec.methodBuilder(model.uniqueSetterMethodName("builder", model.sourceElement().asType()))
        .addParameter(param)
        .returns(model.generatedClass)
        .addModifiers(STATIC)
        .addModifiers(model.maybePublic())
        .addStatement("return $L($N)", toBuilderMethodName, param)
        .build();
  }

  private static MethodSpec buildMethod(
      Model model,
      FieldSpec inUse,
      List<Parameter> parameters) {
    ParameterSpec result = ParameterSpec.builder(TypeName.get(model.sourceElement().asType()), "result")
        .build();
    List<CodeBlock> invocation = parameters.stream()
        .map(Parameter::extract)
        .collect(Collectors.toList());
    MethodSpec.Builder spec = MethodSpec.methodBuilder("build");
    spec.addStatement(CodeBlock.builder().add("$T $N = new $T(", TypeName.get(model.sourceElement().asType()), result, model.avElement)
        .add(invocation.stream().collect(joinCodeBlocks(", $Z")))
        .add(")").build());
    if (model.reuse) {
      parameters.stream().map(Parameter::cleanupCode).forEach(spec::addCode);
      spec.addStatement("$N = $L", inUse, false);
    }
    spec.addModifiers(model.maybePublic());
    if (model.postBuild.isPresent()) {
      ExecutableElement postBuild = model.postBuild.get();
      for (TypeMirror thrownType : postBuild.getThrownTypes()) {
        spec.addException(TypeName.get(thrownType));
      }
      if (postBuild.getReturnType().getKind() == TypeKind.VOID) {
        return spec.addStatement("$N.$L()", result, postBuild.getSimpleName().toString())
            .build();
      } else {
        return spec.returns(TypeName.get(postBuild.getReturnType()))
            .addStatement("return $N.$L()", result, postBuild.getSimpleName().toString())
            .build();
      }
    } else {
      return spec.addStatement("return $N", result)
          .returns(TypeName.get(model.sourceElement().asType()))
          .build();
    }
  }

  private CodeBlock generatedInfo() {
    return CodeBlock.builder().add("Generated by " +
        "<a href=\"https://github.com/h908714124/auto-builder\">\nauto-builder " +
        getClass().getPackage().getImplementationVersion() +
        "</a>\n").build();
  }
}
