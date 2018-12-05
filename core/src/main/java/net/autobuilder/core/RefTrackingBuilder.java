package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.Optional;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.autobuilder.core.AutoBuilderProcessor.rawType;

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
    this.inUse = FieldSpec.builder(TypeName.BOOLEAN, "inUse").build();
  }

  static ClassName perThreadFactoryClass(Model model) {
    return rawType(model.generatedClass)
        .nestedClass("PerThreadFactory");
  }

  static Optional<RefTrackingBuilder> create(Model model, MethodSpec staticBuildMethod) {
    return model.optionalRefTrackingBuilderClass.map(refTrackingBuilderClass -> {
      ClassName perThreadFactoryClass = perThreadFactoryClass(model);
      return new RefTrackingBuilder(model, staticBuildMethod,
          refTrackingBuilderClass, perThreadFactoryClass);
    });
  }

  TypeSpec define() {
    return TypeSpec.classBuilder(refTrackingBuilderClass)
        .addField(inUse)
        .superclass(model.generatedClass)
        .addMethod(buildMethod())
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build();
  }

  private MethodSpec buildMethod() {
    ParameterSpec result = ParameterSpec.builder(TypeName.get(model.sourceClass().asType()), "result").build();
    CodeBlock.Builder builder = CodeBlock.builder()
        .addStatement("$T $N = $T.$N(this)", model.sourceClass(), result,
            rawType(model.generatedClass), staticBuildMethod);
    builder.addStatement("this.$N = $L", inUse, false)
        .addStatement("return $N", result);
    return MethodSpec.methodBuilder("build")
        .addAnnotation(Override.class)
        .addCode(builder.build())
        .returns(TypeName.get(model.sourceClass().asType()))
        .addModifiers(model.maybePublic())
        .build();
  }
}
