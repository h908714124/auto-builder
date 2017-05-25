package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.autobuilder.core.Collectionish.CollectionType.LIST;
import static net.autobuilder.core.Collectionish.CollectionType.MAP;
import static net.autobuilder.core.GuavaCollection.ofGuava;
import static net.autobuilder.core.Util.downcase;
import static net.autobuilder.core.Util.typeArgumentSubtypes;
import static net.autobuilder.core.Util.upcase;
import static net.autobuilder.core.UtilCollection.ofUtil;

final class Collectionish extends ParaParameter {

  private static final ClassName ENTRY_CLASS = ClassName.get(Map.Entry.class);

  enum CollectionType {
    LIST(1, "addTo"), MAP(2, "putIn");
    final int typeParams;
    final String accumulatorPrefix;

    CollectionType(int typeParams, String accumulatorPrefix) {
      this.typeParams = typeParams;
      this.accumulatorPrefix = accumulatorPrefix;
    }
  }

  private static final Map<ClassName, Base> KNOWN = map(
      ofUtil(List.class, "emptyList", ArrayList.class, LIST),
      ofUtil(Set.class, "emptySet", HashSet.class, LIST),
      ofUtil(Map.class, "emptyMap", HashMap.class, MAP),
      ofGuava("ImmutableList", Iterable.class, LIST),
      ofGuava("ImmutableSet", Iterable.class, LIST),
      ofGuava("ImmutableMap", Map.class, MAP));

  static abstract class Base {

    final ClassName className;

    private final Function<FieldSpec, CodeBlock> builderInitBlock;

    abstract CodeBlock emptyBlock();
    abstract ParameterizedTypeName accumulatorType(Parameter parameter);
    abstract Optional<ParameterizedTypeName> addAllType(Parameter parameter);
    abstract CodeBlock setterAssignment(Parameter parameter);
    abstract CodeBlock buildBlock(ParameterSpec builder, FieldSpec field);

    final CollectionType collectionType;

    private final ClassName setterParameterClassName;
    private final boolean wildTyping;

    Base(ClassName className,
         Function<FieldSpec, CodeBlock> builderInitBlock,
         CollectionType collectionType,
         ClassName setterParameterClassName,
         boolean wildTyping) {
      this.className = className;
      this.builderInitBlock = builderInitBlock;
      this.collectionType = collectionType;
      this.setterParameterClassName = setterParameterClassName;
      this.wildTyping = wildTyping;
    }
  }

  private final Base base;

  final Parameter parameter;

  private Collectionish(Base base, Parameter parameter) {
    this.base = base;
    this.parameter = parameter;
  }

  static Optional<ParaParameter> create(Parameter parameter) {
    if (!(parameter.type instanceof ParameterizedTypeName)) {
      return Optional.empty();
    }
    ParameterizedTypeName type = (ParameterizedTypeName) parameter.type;
    Base base = KNOWN.get(type.rawType);
    if (base == null) {
      return Optional.empty();
    }
    if (base.collectionType.typeParams != type.typeArguments.size()) {
      return Optional.empty();
    }
    return Optional.of(new Collectionish(base, parameter));
  }

  static Optional<CodeBlock> emptyBlock(Parameter parameter, ParameterSpec builder) {
    if (!(parameter.type instanceof ParameterizedTypeName)) {
      return Optional.empty();
    }
    ParameterizedTypeName type = (ParameterizedTypeName) parameter.type;
    Base base = KNOWN.get(type.rawType);
    if (base == null) {
      return Optional.empty();
    }
    FieldSpec field = parameter.asField();
    return Optional.of(CodeBlock.builder()
        .add("$N.$N != null ? $N.$N : ",
            builder, field, builder, field)
        .add(base.emptyBlock())
        .build());
  }

  private static Map<ClassName, Base> map(Base... bases) {
    Map<ClassName, Base> map = new HashMap<>(bases.length);
    for (Base base : bases) {
      map.put(base.className, base);
    }
    return map;
  }

  MethodSpec accumulatorMethod() {
    return base.collectionType == CollectionType.MAP ?
        putInMethod() :
        addToMethod();
  }

  Optional<MethodSpec> accumulatorMethodOverload() {
    return base.addAllType(parameter).map(addAllType ->
        base.collectionType == CollectionType.MAP ?
            putAllInMethod(addAllType) :
            addAllToMethod(addAllType));
  }

  CodeBlock getFieldValue(ParameterSpec builder) {
    FieldSpec field = parameter.asField();
    FieldSpec builderField = asBuilderField();
    return CodeBlock.builder()
        .add("$N.$N != null ? ", builder, builderField)
        .add(base.buildBlock(builder, builderField))
        .add(" :\n        ")
        .add("$N.$N != null ? $N.$N : ",
            builder, field, builder, field)
        .add(base.emptyBlock())
        .build();
  }

  Collectionish withParameter(Parameter parameter) {
    return new Collectionish(base, parameter);
  }

  CodeBlock setterAssignment() {
    return base.setterAssignment(parameter);
  }

  String builderFieldName() {
    return downcase(parameter.setterName) + "Builder";
  }

  FieldSpec asBuilderField() {
    return FieldSpec.builder(base.accumulatorType(parameter),
        builderFieldName()).addModifiers(PRIVATE).build();
  }

  String accumulatorName() {
    return base.collectionType.accumulatorPrefix + upcase(parameter.setterName);
  }

  ParameterSpec asParameter() {
    TypeName type = base.wildTyping ?
        ParameterizedTypeName.get(base.setterParameterClassName,
            typeArgumentSubtypes(
                parameter.variableElement)) :
        parameter.type;
    return ParameterSpec.builder(type, parameter.setterName).build();
  }

  private CodeBlock normalAddAll(CodeBlock what) {
    FieldSpec builderField = asBuilderField();
    return CodeBlock.builder()
        .addStatement("this.$N.addAll($L)",
            builderField, what)
        .build();
  }

  private CodeBlock normalPutAll(CodeBlock what) {
    FieldSpec builderField = asBuilderField();
    return CodeBlock.builder()
        .addStatement("this.$N.putAll($L)",
            builderField, what)
        .build();
  }

  static Optional<ParameterizedTypeName> normalAddAllType(
      Parameter parameter,
      CollectionType type,
      ClassName accumulatorAddAllType) {
    ParameterizedTypeName typeName =
        (ParameterizedTypeName) parameter.type;
    if (type.typeParams == 1 &&
        typeName.rawType.equals(accumulatorAddAllType)) {
      return Optional.empty();
    }
    TypeName[] typeArguments = typeArgumentSubtypes(
        parameter.variableElement);
    if (type == Collectionish.CollectionType.LIST) {
      return Optional.of(ParameterizedTypeName.get(
          accumulatorAddAllType, typeArguments));
    }
    return Optional.of(
        ParameterizedTypeName.get(accumulatorAddAllType,
            WildcardTypeName.subtypeOf(ParameterizedTypeName.get(
                ENTRY_CLASS, typeArguments))));
  }

  private String addMethod() {
    return base.collectionType == LIST ? "add" : "put";
  }

  private CodeBlock addAllBlock(CodeBlock what) {
    return base.collectionType == LIST ?
        normalAddAll(what) :
        normalPutAll(what);
  }

  private MethodSpec addAllToMethod(
      ParameterizedTypeName addAllType) {
    FieldSpec field = parameter.asField();
    FieldSpec builderField = asBuilderField();
    ParameterSpec values =
        ParameterSpec.builder(addAllType, "values").build();
    CodeBlock.Builder block = CodeBlock.builder();
    block.beginControlFlow("if ($N == null)", values)
        .addStatement("return this")
        .endControlFlow();
    block.beginControlFlow("if (this.$N == null)", builderField)
        .add(base.builderInitBlock.apply(builderField))
        .endControlFlow();
    block.beginControlFlow("if (this.$N != null)", field)
        .add(addAllBlock(CodeBlock.of("this.$N", field)))
        .addStatement("this.$N = null", field)
        .endControlFlow();
    block.add(addAllBlock(CodeBlock.of("$N", values)));
    return MethodSpec.methodBuilder(
        accumulatorName())
        .addCode(block.build())
        .addStatement("return this")
        .addParameter(values)
        .addModifiers(FINAL)
        .addModifiers(parameter.model.maybePublic())
        .returns(parameter.model.generatedClass)
        .build();
  }

  private MethodSpec putAllInMethod(
      ParameterizedTypeName addAllType) {
    FieldSpec field = parameter.asField();
    FieldSpec builderField = asBuilderField();
    ParameterSpec map =
        ParameterSpec.builder(addAllType, "map").build();
    CodeBlock.Builder block = CodeBlock.builder();
    block.beginControlFlow("if ($N == null)", map)
        .addStatement("return this")
        .endControlFlow();
    block.beginControlFlow("if (this.$N == null)", builderField)
        .add(base.builderInitBlock.apply(builderField))
        .endControlFlow();
    block.beginControlFlow("if (this.$N != null)", field)
        .add(addAllBlock(CodeBlock.of("this.$N", field)))
        .addStatement("this.$N = null", field)
        .endControlFlow();
    block.add(addAllBlock(CodeBlock.of("$N", map)));
    return MethodSpec.methodBuilder(
        accumulatorName())
        .addCode(block.build())
        .addStatement("return this")
        .addParameter(map)
        .addModifiers(FINAL)
        .addModifiers(parameter.model.maybePublic())
        .returns(parameter.model.generatedClass)
        .build();
  }

  private MethodSpec addToMethod() {
    FieldSpec field = parameter.asField();
    FieldSpec builderField = asBuilderField();
    ParameterizedTypeName accumulatorType = base.accumulatorType(parameter);
    ParameterSpec key =
        ParameterSpec.builder(accumulatorType.typeArguments.get(0), "value").build();
    CodeBlock.Builder block = CodeBlock.builder();
    block.beginControlFlow("if (this.$N == null)", builderField)
        .add(base.builderInitBlock.apply(builderField))
        .endControlFlow();
    block.beginControlFlow("if (this.$N != null)", field)
        .add(addAllBlock(CodeBlock.of("this.$N", field)))
        .addStatement("this.$N = null", field)
        .endControlFlow();
    block.addStatement("this.$N.$L($N)",
        builderField, addMethod(), key);
    return MethodSpec.methodBuilder(
        accumulatorName())
        .addCode(block.build())
        .addStatement("return this")
        .addParameter(key)
        .addModifiers(FINAL)
        .addModifiers(parameter.model.maybePublic())
        .returns(parameter.model.generatedClass)
        .build();
  }

  private MethodSpec putInMethod() {
    FieldSpec field = parameter.asField();
    FieldSpec builderField = asBuilderField();
    ParameterizedTypeName accumulatorType = base.accumulatorType(parameter);
    ParameterSpec key =
        ParameterSpec.builder(accumulatorType.typeArguments.get(0), "key").build();
    ParameterSpec value =
        ParameterSpec.builder(accumulatorType.typeArguments.get(1), "value").build();
    CodeBlock.Builder block = CodeBlock.builder();
    block.beginControlFlow("if (this.$N == null)", builderField)
        .add(base.builderInitBlock.apply(builderField))
        .endControlFlow();
    block.beginControlFlow("if (this.$N != null)", field)
        .add(addAllBlock(CodeBlock.of("this.$N", field)))
        .addStatement("this.$N = null", field)
        .endControlFlow();
    block.addStatement("this.$N.$L($N, $N)",
        builderField, addMethod(), key, value);
    return MethodSpec.methodBuilder(
        accumulatorName())
        .addCode(block.build())
        .addStatement("return this")
        .addParameters(asList(key, value))
        .addModifiers(FINAL)
        .addModifiers(parameter.model.maybePublic())
        .returns(parameter.model.generatedClass)
        .build();
  }

  @Override
  <R, P> R accept(Cases<R, P> cases, P p) {
    return cases.collectionish(this, p);
  }
}
