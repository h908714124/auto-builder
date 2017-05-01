package net.autobuilder.core;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.Generated;
import javax.lang.model.element.VariableElement;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.autobuilder.core.Processor.rawType;

final class Analyser {

  private final Model model;

  private Analyser(Model model) {
    this.model = model;
  }

  static Analyser create(Model model) {
    return new Analyser(model);
  }

  TypeSpec analyse() {
    TypeSpec.Builder builder = TypeSpec.classBuilder(rawType(model.generatedClass));
    builder.addTypeVariables(model.typevars);
    builder.addMethod(builderMethod());
    builder.addMethod(builderMethodWithParam());
    builder.addMethod(buildMethod());
    for (VariableElement variableElement : model.avConstructor.getParameters()) {
      FieldSpec f = FieldSpec.builder(TypeName.get(variableElement.asType()),
          variableElement.getSimpleName().toString())
          .addModifiers(PRIVATE)
          .build();
      ParameterSpec p = ParameterSpec.builder(TypeName.get(variableElement.asType()),
          variableElement.getSimpleName().toString()).build();
      builder.addField(f);
      builder.addMethod(MethodSpec.methodBuilder(
          variableElement.getSimpleName().toString())
          .addStatement("this.$N = $N", f, p)
          .addStatement("return this")
          .addParameter(p)
          .addModifiers(PUBLIC)
          .returns(model.generatedClass)
          .build());
    }
    return builder.addModifiers(PUBLIC, FINAL)
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(PRIVATE).build())
        .addAnnotation(AnnotationSpec.builder(Generated.class)
            .addMember("value", "$S", Processor.class.getCanonicalName())
            .build())
        .addJavadoc("Builder for {@link $T}\n", model.sourceClass)
        .build();
  }

  private MethodSpec builderMethod() {
    return MethodSpec.methodBuilder("builder")
        .addModifiers(PUBLIC, STATIC)
        .addTypeVariables(model.typevars)
        .addStatement("return new $T()", model.generatedClass)
        .returns(model.generatedClass)
        .addJavadoc("Creates a new builder.\n" +
            "\n" +
            "@return a builder\n")
        .build();
  }

  private MethodSpec builderMethodWithParam() {
    ParameterSpec builder = ParameterSpec.builder(model.generatedClass, "builder").build();
    ParameterSpec input = ParameterSpec.builder(model.sourceClass, "input").build();
    CodeBlock.Builder block = CodeBlock.builder();
    block.beginControlFlow("if ($N == null)", input)
        .addStatement("throw new $T($S)",
            NullPointerException.class, "Null " + input.name)
        .endControlFlow();
    block.addStatement("$T $N = new $T()", builder.type, builder, model.generatedClass);
    for (VariableElement parameter : model.avConstructor.getParameters()) {
      String name = parameter.getSimpleName().toString();
      block.addStatement("$N.$N = $N.$L()", builder, name, input,
          model.getters.get(name).getSimpleName().toString());
    }
    block.addStatement("return $N", builder);
    return MethodSpec.methodBuilder("builder")
        .addCode(block.build())
        .addParameter(input)
        .addModifiers(PUBLIC, STATIC)
        .addTypeVariables(model.typevars)
        .returns(model.generatedClass)
        .addJavadoc("Creates a new builder.\n" +
                "\n" +
                "@param $N the data source\n" +
                "@return a builder for creating a modified copy\n" +
                "@throws $T if the input is null\n",
            input, NullPointerException.class)
        .build();
  }

  private MethodSpec buildMethod() {
    CodeBlock.Builder block = CodeBlock.builder();
    for (int i = 0; i < model.avConstructor.getParameters().size(); i++) {
      VariableElement variableElement = model.avConstructor.getParameters().get(i);
      FieldSpec f = FieldSpec.builder(TypeName.get(variableElement.asType()),
          variableElement.getSimpleName().toString())
          .addModifiers(PRIVATE)
          .build();
      if (i > 0) {
        block.add(",");
      }
      block.add("\n    $N", f);
    }
    return MethodSpec.methodBuilder("build")
        .addCode("return new $T(", model.avType)
        .addCode(block.build())
        .addCode(");\n")
        .returns(model.sourceClass)
        .addModifiers(PUBLIC)
        .build();
  }
}
