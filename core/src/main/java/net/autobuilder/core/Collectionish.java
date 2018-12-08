package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
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
import static net.autobuilder.core.Collectionish.CollectionType.LIST;
import static net.autobuilder.core.Collectionish.CollectionType.MAP;
import static net.autobuilder.core.GuavaCollection.ofGuava;
import static net.autobuilder.core.Util.className;
import static net.autobuilder.core.Util.downcase;
import static net.autobuilder.core.Util.upcase;
import static net.autobuilder.core.UtilCollection.ofUtil;

public final class Collectionish extends ParaParameter {

  enum CollectionType {

    LIST(1), MAP(2);

    final int numberOfTypeargs;

    CollectionType(int numberOfTypeargs) {
      this.numberOfTypeargs = numberOfTypeargs;
    }
  }

  static abstract class Base {

    private final String collectionClassName;

    private final String overloadArgumentType;

    final CollectionType collectionType;

    abstract CodeBlock accumulatorInitBlock(FieldSpec builderField);

    abstract CodeBlock emptyBlock();

    abstract DeclaredType accumulatorType(Parameter parameter);

    abstract ParameterizedTypeName accumulatorOverloadArgumentType(Parameter parameter);

    abstract CodeBlock setterAssignment(Parameter parameter);

    abstract CodeBlock buildBlock(FieldSpec field);

    abstract ParameterSpec setterParameter(Parameter parameter);

    Base(String collectionClassName,
         String overloadArgumentType,
         CollectionType collectionType) {
      this.collectionClassName = collectionClassName;
      this.overloadArgumentType = overloadArgumentType;
      this.collectionType = collectionType;
    }

    ClassName overloadArgumentType() {
      return className(overloadArgumentType);
    }

    ClassName collectionClassName() {
      return className(collectionClassName);
    }

    String sCollectionClassName() {
      return collectionClassName;
    }
  }

  private static final class LookupResult {
    final Base base;
    final DeclaredType declaredType;

    LookupResult(Base base, DeclaredType declaredType) {
      this.base = base;
      this.declaredType = declaredType;
    }
  }

  private static final Map<String, Base> LOOKUP = createLookup(
      ofUtil("List", "emptyList", ArrayList.class, LIST),
      ofUtil("Set", "emptySet", HashSet.class, LIST),
      ofUtil("Map", "emptyMap", HashMap.class, MAP),
      ofGuava("ImmutableList", Iterable.class, LIST),
      ofGuava("ImmutableSet", Iterable.class, LIST),
      ofGuava("ImmutableMap", Map.class, MAP));

  private final Base base;

  public final Parameter parameter;

  private Collectionish(Base base, Parameter parameter) {
    this.base = base;
    this.parameter = parameter;
  }

  /**
   * @return a collectionish parameter, if this parameter
   * represents a collection type, or else {@link Optional#empty()}
   */
  static Optional<ParaParameter> maybeCreate(Parameter parameter) {
    return lookup(parameter).map(lookupResult ->
        new Collectionish(lookupResult.base, parameter));
  }

  public Optional<CodeBlock> emptyBlock(Parameter parameter) {
    return lookup(parameter).map(lookupResult -> {
      FieldSpec field = parameter.asField();
      return CodeBlock.builder()
          .add("$N != null ? $N : ",
              field, field)
          .add(lookupResult.base.emptyBlock())
          .build();
    });
  }

  private static Optional<LookupResult> lookup(Parameter parameter) {
    TypeMirror type = parameter.variableElement.asType();
    TypeTool tool = TypeTool.get();
    if (tool.hasWildcards(type)) {
      return Optional.empty();
    }
    if (type.getKind() != TypeKind.DECLARED) {
      return Optional.empty();
    }
    DeclaredType declaredType = Util.asDeclared(type);
    return tool.getTypeElement(type)
        .map(TypeElement::getQualifiedName)
        .map(Name::toString)
        .map(LOOKUP::get)
        .filter(base -> base.collectionType.numberOfTypeargs ==
            declaredType.getTypeArguments().size())
        .filter(base -> base.collectionType.numberOfTypeargs != 1 ||
            !tool.isSameErasure(declaredType.getTypeArguments().get(0),
                tool.getTypeElement(base.overloadArgumentType).asType()))
        .map(base -> new LookupResult(base, declaredType));
  }

  private static Map<String, Base> createLookup(Base... bases) {
    Map<String, Base> map = new HashMap<>(bases.length);
    for (Base base : bases) {
      map.put(base.collectionClassName, base);
    }
    return map;
  }

  public Optional<MethodSpec> accumulatorMethod(Model model) {
    return base.collectionType == CollectionType.MAP ?
        putInMethod(model) :
        addToMethod(model);
  }

  public Optional<MethodSpec> accumulatorMethodOverload(Model model) {
    ParameterizedTypeName addAllType = base.accumulatorOverloadArgumentType(parameter);
    return base.collectionType == CollectionType.MAP ?
        putAllInMethod(model, ParameterSpec.builder(addAllType, "map").build()) :
        addAllToMethod(model, ParameterSpec.builder(addAllType, "values").build());
  }

  public CodeBlock getFieldValue() {
    FieldSpec field = parameter.asField();
    FieldSpec builderField = asBuilderField();
    return CodeBlock.builder()
        .add("$N != null ? $L : ",
            builderField, base.buildBlock(builderField))
        .add("( $N != null ? $N : $L )",
            field, field, base.emptyBlock())
        .build();
  }

  Collectionish withParameter(Parameter parameter) {
    return new Collectionish(base, parameter);
  }

  public CodeBlock setterAssignment() {
    return base.setterAssignment(parameter);
  }

  String builderFieldName() {
    return downcase(parameter.setterName) + "Builder";
  }

  public FieldSpec asBuilderField() {
    return FieldSpec.builder(TypeName.get(base.accumulatorType(parameter)),
        builderFieldName()).addModifiers(PRIVATE).build();
  }

  public ParameterSpec asSetterParameter() {
    return base.setterParameter(parameter);
  }

  private Optional<MethodSpec> addAllToMethod(
      Model model,
      ParameterSpec param) {
    FieldSpec field = parameter.asField();
    FieldSpec builderField = asBuilderField();
    String methodName = "addTo" + upcase(parameter.setterName);
    if (model.isSetterMethodNameCollision(methodName)) {
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
        .addStatement("this.$N.addAll(this.$N)", asBuilderField(), field)
        .addStatement("this.$N = null", field)
        .endControlFlow();
    spec.addStatement("this.$N.addAll($N)", asBuilderField(), param);
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
    FieldSpec field = parameter.asField();
    FieldSpec builderField = asBuilderField();
    String methodName = "putIn" + upcase(parameter.setterName);
    if (model.isSetterMethodNameCollision(methodName)) {
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
    FieldSpec field = parameter.asField();
    FieldSpec builderField = asBuilderField();
    DeclaredType accumulatorType = base.accumulatorType(parameter);
    ParameterSpec key =
        ParameterSpec.builder(TypeName.get(accumulatorType.getTypeArguments().get(0)), "value").build();
    String methodName = "addTo" + upcase(parameter.setterName);
    if (model.isSetterMethodNameCollision(methodName)) {
      return Optional.empty();
    }
    MethodSpec.Builder spec = MethodSpec.methodBuilder(methodName);
    spec.beginControlFlow("if (this.$N == null)", builderField)
        .addCode(base.accumulatorInitBlock(builderField))
        .endControlFlow();
    spec.beginControlFlow("if (this.$N != null)", field)
        .addStatement("this.$N.addAll(this.$N)", asBuilderField(), field)
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

  private Optional<MethodSpec> putInMethod(Model model) {
    FieldSpec field = parameter.asField();
    FieldSpec builderField = asBuilderField();
    DeclaredType accumulatorType = base.accumulatorType(parameter);
    ParameterSpec key =
        ParameterSpec.builder(TypeName.get(accumulatorType.getTypeArguments().get(0)), "key").build();
    ParameterSpec value =
        ParameterSpec.builder(TypeName.get(accumulatorType.getTypeArguments().get(1)), "value").build();
    String methodName = "putIn" + upcase(parameter.setterName);
    if (model.isSetterMethodNameCollision(methodName)) {
      return Optional.empty();
    }
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
