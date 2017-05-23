package net.autobuilder.core;

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

import static net.autobuilder.core.Collectionish.CollectionType.LIST;
import static net.autobuilder.core.Collectionish.CollectionType.MAP;

final class Collectionish {

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

  final ClassName className;
  final Function<FieldSpec, CodeBlock> builderInitBlock;
  final Supplier<CodeBlock> emptyBlock;
  final CollectionType type;
  final ClassName setterParameterClassName;
  final ClassName builderAddAllType;
  final Function<Parameter, CodeBlock> setterAssignment;
  final Function<Parameter, ParameterizedTypeName> builderType;
  final Function<Parameter, Optional<ParameterizedTypeName>> addAllType;
  final BiFunction<Parameter, CodeBlock, CodeBlock> addAllBlock;
  final BiFunction<ParameterSpec, FieldSpec, CodeBlock> buildBlock;

  final boolean wildTyping;

  private Collectionish(
      ClassName className,
      Function<FieldSpec, CodeBlock> builderInitBlock,
      Supplier<CodeBlock> emptyBlock,
      CollectionType type,
      ClassName setterParameterClassName,
      ClassName builderAddAllType,
      Function<Parameter, CodeBlock> setterAssignment,
      Function<Parameter, ParameterizedTypeName> builderType,
      Function<Parameter, Optional<ParameterizedTypeName>> addAllType,
      BiFunction<Parameter, CodeBlock, CodeBlock> addAllBlock,
      BiFunction<ParameterSpec, FieldSpec, CodeBlock> buildBlock,
      boolean wildTyping) {
    this.className = className;
    this.builderInitBlock = builderInitBlock;
    this.emptyBlock = emptyBlock;
    this.type = type;
    this.setterParameterClassName = setterParameterClassName;
    this.builderAddAllType = builderAddAllType;
    this.setterAssignment = setterAssignment;
    this.builderType = builderType;
    this.addAllType = addAllType;
    this.addAllBlock = addAllBlock;
    this.buildBlock = buildBlock;
    this.wildTyping = wildTyping;
  }

  private static Collectionish ofUtil(
      Class<?> className, String emptyMethod, Class<?> builderClass, CollectionType type) {
    ClassName builderAddAllType = type == LIST ? ClassName.get(Collection.class) : ClassName.get(Map.class);
    return new Collectionish(
        ClassName.get(className),
        builderField ->
            CodeBlock.builder().addStatement("this.$N = new $T()",
                builderField, builderClass).build(),
        () -> CodeBlock.of("$T.$L()", Collections.class, emptyMethod),
        type,
        ClassName.get(className),
        builderAddAllType,
        parameter -> {
          FieldSpec field = parameter.asField();
          ParameterSpec p = parameter.asParameter();
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
            normalAddAllType(type, builderAddAllType) :
            parameter -> {
              TypeName[] typeArguments = Util.typeArgumentSubtypes(parameter.variableElement);
              return Optional.of(
                  ParameterizedTypeName.get(builderAddAllType,
                      typeArguments));
            },
        type == LIST ? normalAddAll() : normalPutAll(),
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
        builderField ->
            CodeBlock.builder().addStatement("this.$N = $T.builder()",
                builderField, className).build(),
        () -> CodeBlock.of("$T.of()", className),
        type,
        ClassName.get(setterParameterClass),
        builderAddAllType,
        parameter -> {
          FieldSpec field = parameter.asField();
          ParameterSpec p = parameter.asParameter();
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
        type == LIST ? normalAddAll() : normalPutAll(),
        (builder, field) -> CodeBlock.of("$N.$N.build()", builder, field),
        true);
  }

  static Collectionish create(TypeName typeName) {
    if (!(typeName instanceof ParameterizedTypeName)) {
      return null;
    }
    ParameterizedTypeName type = (ParameterizedTypeName) typeName;
    Collectionish collectionish = KNOWN.get(type.rawType);
    if (collectionish == null) {
      return null;
    }
    if (collectionish.type.typeParams != type.typeArguments.size()) {
      return null;
    }
    return collectionish;
  }

  private static Map<ClassName, Collectionish> map(Collectionish... collectionishes) {
    Map<ClassName, Collectionish> map = new HashMap<>(collectionishes.length);
    for (Collectionish collectionish : collectionishes) {
      map.put(collectionish.className, collectionish);
    }
    return map;
  }

  Collectionish noBuilder() {
    if (!hasBuilder()) {
      return this;
    }
    return new Collectionish(className, builderInitBlock, emptyBlock, type,
        setterParameterClassName, null, setterAssignment, builderType, addAllType, addAllBlock, buildBlock,
        wildTyping);
  }

  private static BiFunction<Parameter, CodeBlock, CodeBlock> normalAddAll() {
    return (parameter, what) -> {
      FieldSpec builderField = parameter.asBuilderField();
      return CodeBlock.builder()
          .addStatement("this.$N.addAll($L)",
              builderField, what)
          .build();
    };
  }

  private static BiFunction<Parameter, CodeBlock, CodeBlock> normalPutAll() {
    return (parameter, what) -> {
      FieldSpec builderField = parameter.asBuilderField();
      return CodeBlock.builder()
          .addStatement("this.$N.putAll($L)",
              builderField, what)
          .build();
    };
  }

  String addMethod() {
    return type == LIST ? "add" : "put";
  }

  boolean hasBuilder() {
    return builderAddAllType != null;
  }

  private static Function<Parameter, Optional<ParameterizedTypeName>> normalAddAllType(
      CollectionType type,
      ClassName builderAddAllType) {
    return parameter -> {
      ParameterizedTypeName typeName = (ParameterizedTypeName) parameter.type;
      if (type.typeParams == 1 &&
          typeName.rawType.equals(builderAddAllType)) {
        return Optional.empty();
      }
      TypeName[] typeArguments = Util.typeArgumentSubtypes(parameter.variableElement);
      if (type == Collectionish.CollectionType.LIST) {
        return Optional.of(ParameterizedTypeName.get(
            builderAddAllType, typeArguments));
      }
      return Optional.of(
          ParameterizedTypeName.get(builderAddAllType,
              WildcardTypeName.subtypeOf(ParameterizedTypeName.get(
                  ENTRY_CLASS, typeArguments))));
    };
  }
}
