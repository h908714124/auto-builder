package net.autobuilder.core;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.autobuilder.core.CollectionParameter.CollectionType.LIST;
import static net.autobuilder.core.CollectionParameter.CollectionType.MAP;
import static net.autobuilder.core.GuavaCollectionBase.ofGuava;
import static net.autobuilder.core.Util.asDeclared;
import static net.autobuilder.core.Util.downcase;
import static net.autobuilder.core.Util.upcase;
import static net.autobuilder.core.UtilCollectionBase.ofUtil;

public final class CollectionParameter extends Parameter {

  enum CollectionType {

    LIST(1), MAP(2);

    final int numberOfTypeargs;

    CollectionType(int numberOfTypeargs) {
      this.numberOfTypeargs = numberOfTypeargs;
    }
  }

  private static final Map<String, CollectionBase> LOOKUP = createLookup(
      ofUtil("List", "emptyList", ArrayList.class, LIST),
      ofUtil("Set", "emptySet", HashSet.class, LIST),
      ofUtil("Map", "emptyMap", HashMap.class, MAP),
      ofGuava("ImmutableList", Iterable.class, LIST),
      ofGuava("ImmutableSet", Iterable.class, LIST),
      ofGuava("ImmutableMap", Map.class, MAP));

  private final CollectionBase base;

  public final RegularParameter parameter;

  private final boolean degenerate;

  private CollectionParameter(CollectionBase base, RegularParameter parameter, boolean degenerate) {
    this.base = base;
    this.parameter = parameter;
    this.degenerate = degenerate;
  }

  DeclaredType accumulatorOverloadArgumentType() {
    return base.accumulatorOverloadArgumentType(parameter);
  }

  /**
   * @return a collectionish parameter, if this parameter
   * represents a collection type, or else {@link Optional#empty()}
   */
  static Optional<Parameter> maybeCreate(RegularParameter parameter) {
    return lookup(parameter).map(base -> {
      boolean degenerate;
      TypeTool tool = TypeTool.get();
      DeclaredType declared = asDeclared(parameter.type());
      if (tool.hasWildcards(parameter.type())) {
        degenerate = true;
      } else if (base.collectionType.numberOfTypeargs !=
          declared.getTypeArguments().size()) {
        degenerate = true;
      } else if (base.collectionType.numberOfTypeargs == 1 &&
          tool.isSameErasure(
              declared.getTypeArguments().get(0),
              tool.getTypeElement(base.overloadArgumentType).asType())) {
        degenerate = true;
      } else {
        degenerate = false;
      }
      return new CollectionParameter(base, parameter, degenerate);
    });
  }

  private static Optional<CollectionBase> lookup(RegularParameter parameter) {
    TypeMirror type = parameter.variableElement.asType();
    TypeTool tool = TypeTool.get();
    if (type.getKind() != TypeKind.DECLARED) {
      return Optional.empty();
    }
    return tool.getTypeElement(type)
        .map(TypeElement::getQualifiedName)
        .map(Name::toString)
        .map(LOOKUP::get);
  }

  private static Map<String, CollectionBase> createLookup(CollectionBase... bases) {
    Map<String, CollectionBase> map = new HashMap<>(bases.length);
    for (CollectionBase base : bases) {
      map.put(base.collectionClassName, base);
    }
    return map;
  }

  public Optional<MethodSpec> accumulatorMethod(Model model) {
    return base.collectionType == CollectionType.MAP ?
        putInMethod() :
        addToMethod(model);
  }

  public Optional<MethodSpec> accumulatorMethodOverload(Model model) {
    DeclaredType addAllType = base.accumulatorOverloadArgumentType(parameter);
    return base.collectionType == CollectionType.MAP ?
        putAllInMethod(model, ParameterSpec.builder(TypeName.get(addAllType), "map").build()) :
        addAllToMethod(model, ParameterSpec.builder(TypeName.get(addAllType), "values").build());
  }

  public CodeBlock getFieldValue() {
    FieldSpec field = parameter.asField();
    CodeBlock.Builder code = CodeBlock.builder();
    asBuilderField().ifPresent(builderField ->
        code.add("$N != null ? $L : ",
            builderField, base.buildBlock(builderField)));
    code.add("$N != null ? $N : $L",
        field, field, base.emptyBlock());
    return code.build();
  }

  CollectionParameter withParameter(RegularParameter parameter) {
    return new CollectionParameter(base, parameter, degenerate);
  }

  public CodeBlock setterAssignment() {
    return base.setterAssignment(parameter);
  }

  String builderFieldName() {
    return downcase(parameter.setterName) + "Builder";
  }

  public Optional<FieldSpec> asBuilderField() {
    if (degenerate) {
      return Optional.empty();
    }
    return Optional.of(FieldSpec.builder(TypeName.get(base.accumulatorType(parameter)),
        builderFieldName()).addModifiers(PRIVATE).build());
  }

  public ParameterSpec asSetterParameter() {
    return base.setterParameter(parameter);
  }

  private Optional<MethodSpec> addAllToMethod(
      Model model,
      ParameterSpec param) {
    return asBuilderField().flatMap(builderField -> _addAllToMethod(model, param, builderField));
  }

  private Optional<MethodSpec> _addAllToMethod(
      Model model,
      ParameterSpec param,
      FieldSpec builderField) {
    FieldSpec field = parameter.asField();
    String methodName = "addTo" + upcase(parameter.setterName);
    if (model.isSetterMethodNameCollision(methodName, accumulatorOverloadArgumentType())) {
      return Optional.empty();
    }
    MethodSpec.Builder spec = MethodSpec.methodBuilder(methodName);
    spec.beginControlFlow("if ($N == null)", param)
        .addStatement("return this")
        .endControlFlow();
    spec.beginControlFlow("if (this.$N == null)", builderField)
        .addCode(base.accumulatorInitBlock(builderField))
        .endControlFlow();
    spec.beginControlFlow("if (this.$N != null)", field)
        .addStatement("this.$N.addAll(this.$N)", builderField, field)
        .addStatement("this.$N = null", field)
        .endControlFlow();
    spec.addStatement("this.$N.addAll($N)", builderField, param);
    return Optional.of(spec
        .addStatement("return this")
        .addParameter(param)
        .addModifiers(FINAL)
        .addModifiers(parameter.maybePublic())
        .returns(parameter.generatedClass)
        .build());
  }

  private Optional<MethodSpec> putAllInMethod(
      Model model, ParameterSpec param) {
    return asBuilderField().flatMap(builderField -> _putAllInMethod(model, param, builderField));
  }

  private Optional<MethodSpec> _putAllInMethod(
      Model model,
      ParameterSpec param,
      FieldSpec builderField) {
    FieldSpec field = parameter.asField();
    String methodName = "putIn" + upcase(parameter.setterName);
    if (model.isSetterMethodNameCollision(methodName, accumulatorOverloadArgumentType())) {
      return Optional.empty();
    }
    MethodSpec.Builder spec = MethodSpec.methodBuilder(methodName);
    spec.beginControlFlow("if ($N == null)", param)
        .addStatement("return this")
        .endControlFlow();
    spec.beginControlFlow("if (this.$N == null)", builderField)
        .addCode(base.accumulatorInitBlock(builderField))
        .endControlFlow();
    spec.beginControlFlow("if (this.$N != null)", field)
        .addStatement("this.$N.putAll(this.$N)", builderField, field)
        .addStatement("this.$N = null", field)
        .endControlFlow();
    spec.addStatement("this.$N.putAll($N)", builderField, param);
    return Optional.of(spec
        .addStatement("return this")
        .addParameter(param)
        .addModifiers(FINAL)
        .addModifiers(parameter.maybePublic())
        .returns(parameter.generatedClass)
        .build());
  }

  private Optional<MethodSpec> addToMethod(Model model) {
    return asBuilderField().flatMap(builderField -> _addToMethod(model, builderField));
  }

  private Optional<MethodSpec> _addToMethod(Model model, FieldSpec builderField) {
    FieldSpec field = parameter.asField();
    DeclaredType accumulatorType = base.accumulatorType(parameter);
    ParameterSpec key =
        ParameterSpec.builder(TypeName.get(accumulatorType.getTypeArguments().get(0)), "value").build();
    String methodName = "addTo" + upcase(parameter.setterName);
    if (model.isSetterMethodNameCollision(methodName, accumulatorType.getTypeArguments().get(0))) {
      return Optional.empty();
    }
    MethodSpec.Builder spec = MethodSpec.methodBuilder(methodName);
    spec.beginControlFlow("if (this.$N == null)", builderField)
        .addCode(base.accumulatorInitBlock(builderField))
        .endControlFlow();
    spec.beginControlFlow("if (this.$N != null)", field)
        .addStatement("this.$N.addAll(this.$N)", builderField, field)
        .addStatement("this.$N = null", field)
        .endControlFlow();
    spec.addStatement("this.$N.add($N)",
        builderField, key);
    return Optional.of(spec.addStatement("return this")
        .addParameter(key)
        .addModifiers(FINAL)
        .addModifiers(parameter.maybePublic())
        .returns(parameter.generatedClass)
        .build());
  }

  private Optional<MethodSpec> putInMethod() {
    return asBuilderField().flatMap(this::_putInMethod);
  }

  private Optional<MethodSpec> _putInMethod(FieldSpec builderField) {
    FieldSpec field = parameter.asField();
    DeclaredType accumulatorType = base.accumulatorType(parameter);
    ParameterSpec key =
        ParameterSpec.builder(TypeName.get(accumulatorType.getTypeArguments().get(0)), "key").build();
    ParameterSpec value =
        ParameterSpec.builder(TypeName.get(accumulatorType.getTypeArguments().get(1)), "value").build();
    String methodName = "putIn" + upcase(parameter.setterName);
    MethodSpec.Builder spec = MethodSpec.methodBuilder(methodName);
    spec.beginControlFlow("if (this.$N == null)", builderField)
        .addCode(base.accumulatorInitBlock(builderField))
        .endControlFlow();
    spec.beginControlFlow("if (this.$N != null)", field)
        .addStatement("this.$N.putAll(this.$N)", builderField, field)
        .addStatement("this.$N = null", field)
        .endControlFlow();
    spec.addStatement("this.$N.put($N, $N)",
        builderField, key, value);
    return Optional.of(spec.addStatement("return this")
        .addParameters(asList(key, value))
        .addModifiers(FINAL)
        .addModifiers(parameter.maybePublic())
        .returns(parameter.generatedClass)
        .build());
  }

  @Override
  <R, P> R accept(ParamCases<R, P> cases, P p) {
    return cases.collectionish(this, p);
  }
}
