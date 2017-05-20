package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static net.autobuilder.core.Collectionish.CollectionType.LIST;
import static net.autobuilder.core.Collectionish.CollectionType.MAP;

final class Collectionish {

  private static final String GCC = "com.google.common.collect";

  enum CollectionType {
    LIST(1, "addTo"), MAP(2, "putIn");
    final int typeParams;
    final String aggregatorPrefix;
    CollectionType(int typeParams, String aggregatorPrefix) {
      this.typeParams = typeParams;
      this.aggregatorPrefix = aggregatorPrefix;
    }
  }

  private static final Map<ClassName, Collectionish> KNOWN = map(
      of(List.class, Collections.class, "emptyList", LIST),
      of(Set.class, Collections.class, "emptySet", LIST),
      of(Map.class, Collections.class, "emptyMap", MAP),
      ofGuava("ImmutableList", Iterable.class, LIST),
      ofGuava("ImmutableSet", Iterable.class, LIST),
      ofGuava("ImmutableMap", Map.class, MAP));

  final ClassName className;
  final ClassName factoryClassName;
  final String emptyMethod;
  final String addMethod;
  final String addAllMethod;
  final CollectionType type;
  final ClassName setterParameterClassName;
  final Function<Parameter, CodeBlock> setterAssignment;
  final boolean wildTyping;

  private Collectionish(
      ClassName className,
      ClassName factoryClassName,
      String emptyMethod,
      String addMethod,
      String addAllMethod,
      CollectionType type,
      ClassName setterParameterClassName,
      Function<Parameter, CodeBlock> setterAssignment, boolean wildTyping) {
    this.className = className;
    this.factoryClassName = factoryClassName;
    this.emptyMethod = emptyMethod;
    this.addMethod = addMethod;
    this.addAllMethod = addAllMethod;
    this.type = type;
    this.setterParameterClassName = setterParameterClassName;
    this.setterAssignment = setterAssignment;
    this.wildTyping = wildTyping;
  }

  private static Collectionish of(
      Class<?> className, Class<?> factoryClassName, String emptyMethod, CollectionType type) {
    return new Collectionish(
        ClassName.get(className),
        ClassName.get(factoryClassName),
        emptyMethod, null, null, type, ClassName.get(className), null, false);
  }

  private static Collectionish ofGuava(
      String simpleName,
      Class<?> setterParameterClass,
      CollectionType type) {
    ClassName className = ClassName.get(GCC, simpleName);
    return new Collectionish(className,
        className, "of",
        type == LIST ? "add" : "put",
        type == LIST ? "addAll" : "putAll",
        type, ClassName.get(setterParameterClass),
        parameter -> {
          FieldSpec field = parameter.asField().build();
          ParameterSpec p = parameter.asParameter();
          return CodeBlock.builder()
              .addStatement("this.$N = $N != null ? $T.copyOf($N) : null",
                  field, p, className, p)
              .build();
        }, true);
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
    return new Collectionish(className, factoryClassName, emptyMethod, null, addAllMethod, type, setterParameterClassName, setterAssignment, wildTyping);
  }

  boolean hasBuilder() {
    return addMethod != null;
  }
}
