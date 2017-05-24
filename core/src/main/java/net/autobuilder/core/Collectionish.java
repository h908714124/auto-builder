package net.autobuilder.core;

import static javax.lang.model.element.Modifier.PRIVATE;
import static net.autobuilder.core.Collectionish.CollectionType.LIST;
import static net.autobuilder.core.Collectionish.CollectionType.MAP;
import static net.autobuilder.core.Util.downcase;
import static net.autobuilder.core.Util.typeArgumentSubtypes;
import static net.autobuilder.core.Util.upcase;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

final class Collectionish extends ParaParameter {

  private static final String GCC = "com.google.common.collect";
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

  private static final Map<ClassName, Collectionish> KNOWN = map(
      ofUtil(List.class, "emptyList", ArrayList.class, LIST),
      ofUtil(Set.class, "emptySet", HashSet.class, LIST),
      ofUtil(Map.class, "emptyMap", HashMap.class, MAP),
      ofGuava("ImmutableList", Iterable.class, LIST),
      ofGuava("ImmutableSet", Iterable.class, LIST),
      ofGuava("ImmutableMap", Map.class, MAP));

  private final ClassName className;
  private final ClassName accumulatorAddAllType;

  final Parameter parameter;
  final Function<FieldSpec, CodeBlock> builderInitBlock;
  final Supplier<CodeBlock> emptyBlock;
  final CollectionType type;
  final ClassName setterParameterClassName;
  final Function<Collectionish, CodeBlock> setterAssignment;
  final Function<Parameter, ParameterizedTypeName> accumulatorType;
  final Function<Collectionish, Optional<ParameterizedTypeName>> addAllType;
  final BiFunction<Collectionish, CodeBlock, CodeBlock> addAllBlock;
  final BiFunction<ParameterSpec, FieldSpec, CodeBlock> buildBlock;

  final boolean wildTyping;

  private Collectionish(
      ClassName className,
      Parameter parameter,
      Function<FieldSpec, CodeBlock> builderInitBlock,
      Supplier<CodeBlock> emptyBlock,
      CollectionType type,
      ClassName setterParameterClassName,
      ClassName accumulatorAddAllType,
      Function<Collectionish, CodeBlock> setterAssignment,
      Function<Parameter, ParameterizedTypeName> accumulatorType,
      Function<Collectionish, Optional<ParameterizedTypeName>> addAllType,
      BiFunction<Collectionish, CodeBlock, CodeBlock> addAllBlock,
      BiFunction<ParameterSpec, FieldSpec, CodeBlock> buildBlock,
      boolean wildTyping) {
    this.className = className;
    this.parameter = parameter;
    this.builderInitBlock = builderInitBlock;
    this.emptyBlock = emptyBlock;
    this.type = type;
    this.setterParameterClassName = setterParameterClassName;
    this.accumulatorAddAllType = accumulatorAddAllType;
    this.setterAssignment = setterAssignment;
    this.accumulatorType = accumulatorType;
    this.addAllType = addAllType;
    this.addAllBlock = addAllBlock;
    this.buildBlock = buildBlock;
    this.wildTyping = wildTyping;
  }

  private static Collectionish ofUtil(
      Class<?> className, String emptyMethod, Class<?> builderClass, CollectionType type) {
    ClassName accumulatorAddAllType = type == LIST ? ClassName.get(Collection.class) : ClassName.get(Map.class);
    return new Collectionish(
        ClassName.get(className),
        null,
        builderField ->
            CodeBlock.builder().addStatement("this.$N = new $T<>()",
                builderField, builderClass).build(),
        () -> CodeBlock.of("$T.$L()", Collections.class, emptyMethod),
        type,
        ClassName.get(className),
        accumulatorAddAllType,
        collectionish -> {
          FieldSpec field = collectionish.parameter.asField();
          ParameterSpec p = ParaParameter.AS_PARAMETER.apply(collectionish);
          return CodeBlock.builder()
              .addStatement("this.$N = $N", field, p)
              .build();
        },
        parameter -> {
          ParameterizedTypeName typeName = (ParameterizedTypeName) parameter.type;
          return ParameterizedTypeName.get(ClassName.get(builderClass),
              typeName.typeArguments.toArray(new TypeName[0]));
        },
        type == LIST ?
            normalAddAllType(type, accumulatorAddAllType) :
            collectionish -> {
              TypeName[] typeArguments = typeArgumentSubtypes(
                  collectionish.parameter.variableElement);
              return Optional.of(
                  ParameterizedTypeName.get(accumulatorAddAllType,
                      typeArguments));
            },
        type == LIST ?
            Collectionish::normalAddAll :
            Collectionish::normalPutAll,
        (builder, field) -> CodeBlock.of("$N.$N", builder, field),
        false);
  }

  private static Collectionish ofGuava(
      String simpleName,
      Class<?> setterParameterClass,
      CollectionType type) {
    ClassName className = ClassName.get(GCC, simpleName);
    ClassName builderAddAllType = ClassName.get(Iterable.class);
    return new Collectionish(
        className,
        null,
        builderField ->
            CodeBlock.builder().addStatement("this.$N = $T.builder()",
                builderField, className).build(),
        () -> CodeBlock.of("$T.of()", className),
        type,
        ClassName.get(setterParameterClass),
        builderAddAllType,
        collectionish -> {
          FieldSpec field = collectionish.parameter.asField();
          ParameterSpec p = ParaParameter.AS_PARAMETER.apply(collectionish);
          return CodeBlock.builder()
              .addStatement("this.$N = $N != null ? $T.copyOf($N) : null",
                  field, p, className, p)
              .build();
        },
        parameter -> {
          ParameterizedTypeName typeName =
              (ParameterizedTypeName) TypeName.get(parameter.variableElement.asType());
          return ParameterizedTypeName.get(className.nestedClass("Builder"),
              typeName.typeArguments.toArray(new TypeName[typeName.typeArguments.size()]));
        },
        normalAddAllType(type, builderAddAllType),
        type == LIST ?
            Collectionish::normalAddAll :
            Collectionish::normalPutAll,
        (builder, field) -> CodeBlock.of("$N.$N.build()", builder, field),
        true);
  }

  static Optional<ParaParameter> create(Parameter parameter) {
    if (!(parameter.type instanceof ParameterizedTypeName)) {
      return Optional.empty();
    }
    ParameterizedTypeName type = (ParameterizedTypeName) parameter.type;
    Collectionish collectionish = KNOWN.get(type.rawType);
    if (collectionish == null) {
      return Optional.empty();
    }
    if (collectionish.type.typeParams != type.typeArguments.size()) {
      return Optional.empty();
    }
    return Optional.of(collectionish.withParameter(parameter));
  }

  private static Map<ClassName, Collectionish> map(Collectionish... collectionishes) {
    Map<ClassName, Collectionish> map = new HashMap<>(collectionishes.length);
    for (Collectionish collectionish : collectionishes) {
      map.put(collectionish.className, collectionish);
    }
    return map;
  }

  Collectionish noAccumulator() {
    if (!hasAccumulator()) {
      return this;
    }
    return new Collectionish(className, parameter, builderInitBlock, emptyBlock, type,
        setterParameterClassName, null, setterAssignment, accumulatorType, addAllType, addAllBlock, buildBlock,
        wildTyping);
  }

  Collectionish withParameter(Parameter parameter) {
    return new Collectionish(className, parameter, builderInitBlock, emptyBlock, type,
        setterParameterClassName, accumulatorAddAllType, setterAssignment, accumulatorType, addAllType, addAllBlock, buildBlock,
        wildTyping);
  }

  private static CodeBlock normalAddAll(Collectionish collectionish, CodeBlock what) {
    FieldSpec builderField = collectionish.asBuilderField();
    return CodeBlock.builder()
        .addStatement("this.$N.addAll($L)",
            builderField, what)
        .build();
  }

  private static CodeBlock normalPutAll(Collectionish collectionish, CodeBlock what) {
    FieldSpec builderField = collectionish.asBuilderField();
    return CodeBlock.builder()
        .addStatement("this.$N.putAll($L)",
            builderField, what)
        .build();
  }

  String addMethod() {
    return type == LIST ? "add" : "put";
  }

  boolean hasAccumulator() {
    return accumulatorAddAllType != null;
  }

  private static Function<Collectionish, Optional<ParameterizedTypeName>> normalAddAllType(
      CollectionType type,
      ClassName accumulatorAddAllType) {
    return collectionish -> {
      ParameterizedTypeName typeName =
          (ParameterizedTypeName) collectionish.parameter.type;
      if (type.typeParams == 1 &&
          typeName.rawType.equals(accumulatorAddAllType)) {
        return Optional.empty();
      }
      TypeName[] typeArguments = typeArgumentSubtypes(
          collectionish.parameter.variableElement);
      if (type == Collectionish.CollectionType.LIST) {
        return Optional.of(ParameterizedTypeName.get(
            accumulatorAddAllType, typeArguments));
      }
      return Optional.of(
          ParameterizedTypeName.get(accumulatorAddAllType,
              WildcardTypeName.subtypeOf(ParameterizedTypeName.get(
                  ENTRY_CLASS, typeArguments))));
    };
  }

  String builderFieldName() {
    if (!hasAccumulator()) {
      throw new AssertionError();
    }
    return downcase(parameter.setterName) + "Builder";
  }

  FieldSpec asBuilderField() {
    return FieldSpec.builder(builderType(),
        builderFieldName()).addModifiers(PRIVATE).build();
  }


  ParameterizedTypeName builderType() {
    return accumulatorType.apply(parameter);
  }

  CodeBlock addAllBlock(CodeBlock what) {
    return addAllBlock.apply(this, what);
  }

  String accumulatorName() {
    return type.accumulatorPrefix + upcase(parameter.setterName);
  }

  @Override
  <R, P> R accept(Cases<R, P> cases, P p) {
    return cases.collectionish(this, p);
  }
}
