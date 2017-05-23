package net.autobuilder.core;

import static net.autobuilder.core.Collectionish.CollectionType.LIST;
import static net.autobuilder.core.Collectionish.CollectionType.MAP;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

final class Collectionish {

  private static final String GCC = "com.google.common.collect";

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
      ofUtil(List.class, "emptyList", LIST),
      ofUtil(Set.class, "emptySet", LIST),
      ofUtil(Map.class, "emptyMap", MAP),
      ofGuava("ImmutableList", Iterable.class, LIST),
      ofGuava("ImmutableSet", Iterable.class, LIST),
      ofGuava("ImmutableMap", Map.class, MAP));

  final ClassName className;
  final Function<FieldSpec, CodeBlock> builderInitBlock;
  final Supplier<CodeBlock> emptyBlock;
  final CollectionType type;
  final ClassName setterParameterClassName;
  final Function<Parameter, CodeBlock> setterAssignment;
  final Function<Parameter, ParameterizedTypeName> builderType;
  final BiFunction<Parameter, CodeBlock, CodeBlock> addAllBlock;

  private final boolean hasBuilder;
  final boolean wildTyping;

  private Collectionish(
      boolean hasBuilder,
      ClassName className,
      Function<FieldSpec, CodeBlock> builderInitBlock,
      Supplier<CodeBlock> emptyBlock,
      CollectionType type,
      ClassName setterParameterClassName,
      Function<Parameter, CodeBlock> setterAssignment,
      Function<Parameter, ParameterizedTypeName> builderType,
      BiFunction<Parameter, CodeBlock, CodeBlock> addAllBlock,
      boolean wildTyping) {
    this.hasBuilder = hasBuilder;
    this.className = className;
    this.builderInitBlock = builderInitBlock;
    this.emptyBlock = emptyBlock;
    this.type = type;
    this.setterParameterClassName = setterParameterClassName;
    this.setterAssignment = setterAssignment;
    this.builderType = builderType;
    this.addAllBlock = addAllBlock;
    this.wildTyping = wildTyping;
  }

  private static Collectionish ofUtil(
      Class<?> className, String emptyMethod, CollectionType type) {
    ClassName builderClass = type == LIST ?
        ClassName.get(ArrayList.class) :
        ClassName.get(HashMap.class);
    return new Collectionish(
        true,
        ClassName.get(className),
        builderField ->
            CodeBlock.builder().addStatement("this.$N = new $T()",
                builderField, builderClass).build(),
        () -> CodeBlock.of("$T.$L()", Collections.class, emptyMethod),
        type,
        ClassName.get(className),
        parameter -> {
          FieldSpec field = parameter.asField();
          ParameterSpec p = parameter.asParameter();
          return CodeBlock.builder()
              .addStatement("this.$N = $N", field, p)
              .build();
        },
        parameter -> {
          ParameterizedTypeName typeName = (ParameterizedTypeName) parameter.type;
          return ParameterizedTypeName.get(builderClass,
              typeName.typeArguments.toArray(new TypeName[0]));
        },
        normalAddAll(),
        false);
  }

  private static Collectionish ofGuava(
      String simpleName,
      Class<?> setterParameterClass,
      CollectionType type) {
    ClassName className = ClassName.get(GCC, simpleName);
    return new Collectionish(
        true,
        className,
        builderField ->
            CodeBlock.builder().addStatement("this.$N = $T.builder()",
                builderField, className).build(),
        () -> CodeBlock.of("$T.of()", className),
        type,
        ClassName.get(setterParameterClass),
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
        type == LIST ? normalAddAll() : guavaPutAll(),
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
    if (!hasBuilder) {
      return this;
    }
    return new Collectionish(false, className, builderInitBlock, emptyBlock, type,
        setterParameterClassName, setterAssignment, builderType, addAllBlock, wildTyping);
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

  private static BiFunction<Parameter, CodeBlock, CodeBlock> guavaPutAll() {
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
    return hasBuilder;
  }
}
