package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.Optional;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.autobuilder.core.Processor.rawType;

final class RefTrackingBuilder {

  private final Model model;
  private final MethodSpec staticBuildMethod;
  final FieldSpec inUse;
  final ClassName refTrackingBuilderClass;
  final ClassName perThreadFactoryClass;

  private RefTrackingBuilder(Model model, MethodSpec staticBuildMethod,
                             ClassName refTrackingBuilderClass,
                             ClassName perThreadFactoryClass) {
    this.model = model;
    this.staticBuildMethod = staticBuildMethod;
    this.refTrackingBuilderClass = refTrackingBuilderClass;
    this.perThreadFactoryClass = perThreadFactoryClass;
    this.inUse = FieldSpec.builder(TypeName.BOOLEAN, "inUse", PRIVATE).build();
  }

  static Optional<RefTrackingBuilder> create(Model model, MethodSpec staticBuildMethod) {
    return model.optionalRefTrackingBuilderClass.map(refTrackingBuilderClass -> {
      ClassName perThreadFactoryClass = rawType(model.generatedClass)
          .nestedClass("PerThreadFactory");
      return new RefTrackingBuilder(model, staticBuildMethod,
          refTrackingBuilderClass, perThreadFactoryClass);
    });
  }

  TypeSpec define() {
    return TypeSpec.classBuilder(refTrackingBuilderClass)
        .addField(inUse)
        .superclass(model.generatedClass)
        .addMethod(buildMethod())
        .addModifiers(PUBLIC, STATIC, FINAL)
        .build();
  }

  private MethodSpec buildMethod() {
    ParameterSpec result = ParameterSpec.builder(model.sourceClass, "result").build();
    CodeBlock.Builder builder = CodeBlock.builder()
        .addStatement("$T $N = $T.$N(this)", model.sourceClass, result,
            rawType(model.generatedClass), staticBuildMethod);
    for (Parameter parameter : model.parameters) {
      if (OptionalInfo.isOptional(parameter.type)) {
        builder.addStatement("this.$L($T.empty())",
            parameter.setterName, Optional.class);
      } else if (parameter.type instanceof ClassName ||
          parameter.type instanceof ParameterizedTypeName) {
        builder.addStatement("this.$L(null)",
            parameter.setterName);
      }
    }
    builder.addStatement("this.$N = $L", inUse, false)
        .addStatement("return $N", result);
    return MethodSpec.methodBuilder("build")
        .addAnnotation(Override.class)
        .addCode(builder.build())
        .returns(model.sourceClass)
        .addModifiers(PUBLIC)
        .build();
  }
}
