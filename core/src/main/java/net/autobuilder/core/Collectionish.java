package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.autobuilder.core.Collectionish.CollectionType.LIST;
import static net.autobuilder.core.Collectionish.CollectionType.MAP;

final class Collectionish {

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
      of("com.google.common.collect", "ImmutableList", "of", "add", "addAll", LIST),
      of("com.google.common.collect", "ImmutableSet", "of", "add", "addAll", LIST),
      of("com.google.common.collect", "ImmutableMap", "of", "put", "putAll", MAP));

  final ClassName className;
  final ClassName factoryClassName;
  final String emptyMethod;
  final String addMethod;
  final String addAllMethod;
  final CollectionType type;

  private Collectionish(
      ClassName className,
      ClassName factoryClassName,
      String emptyMethod,
      String addMethod,
      String addAllMethod,
      CollectionType type) {
    this.className = className;
    this.factoryClassName = factoryClassName;
    this.emptyMethod = emptyMethod;
    this.addMethod = addMethod;
    this.addAllMethod = addAllMethod;
    this.type = type;
  }

  private static Collectionish of(
      Class<?> className, Class<?> factoryClassName, String emptyMethod, CollectionType type) {
    return new Collectionish(
        ClassName.get(className),
        ClassName.get(factoryClassName),
        emptyMethod, null, null, type);
  }

  private static Collectionish of(
      String packageName,
      String simpleName,
      String emptyMethod,
      String addMethod,
      String addAllMethod,
      CollectionType type) {
    ClassName className = ClassName.get(packageName, simpleName);
    return new Collectionish(className,
        className, emptyMethod, addMethod, addAllMethod, type);
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
    return new Collectionish(className, factoryClassName, emptyMethod, null, addAllMethod, type);
  }

  boolean hasBuilder() {
    return addMethod != null;
  }
}
