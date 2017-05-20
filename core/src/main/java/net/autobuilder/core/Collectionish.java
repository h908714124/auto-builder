package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Collectionish {

  private static final Map<ClassName, Collectionish> MAP = map(
      of(List.class, Collections.class, "emptyList"),
      of(Map.class, Collections.class, "emptyMap"),
      of(Set.class, Collections.class, "emptySet"),
      of("com.google.common.collect", "ImmutableList", "of", "add", "addAll"),
      of("com.google.common.collect", "ImmutableSet", "of", "add", "addAll"),
      of("com.google.common.collect", "ImmutableMap", "of", "put", "putAll"));

  final ClassName className;
  final ClassName factoryClassName;
  final String emptyMethod;
  final String addMethod;
  final String addAllMethod;

  private Collectionish(
      ClassName className,
      ClassName factoryClassName,
      String emptyMethod,
      String addMethod, String addAllMethod) {
    this.className = className;
    this.factoryClassName = factoryClassName;
    this.emptyMethod = emptyMethod;
    this.addMethod = addMethod;
    this.addAllMethod = addAllMethod;
  }

  private static Collectionish of(Class<?> className, Class<?> factoryClassName, String emptyMethod) {
    return new Collectionish(
        ClassName.get(className),
        ClassName.get(factoryClassName),
        emptyMethod, null, null);
  }

  private static Collectionish of(
      String packageName,
      String simpleName,
      String emptyMethod,
      String addMethod,
      String addAllMethod) {
    ClassName className = ClassName.get(packageName, simpleName);
    return new Collectionish(className,
        className, emptyMethod, addMethod, addAllMethod);
  }

  static Collectionish create(TypeName typeName) {
    if (!(typeName instanceof ParameterizedTypeName)) {
      return null;
    }
    return MAP.get(((ParameterizedTypeName) typeName).rawType);
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
    return new Collectionish(className, factoryClassName, emptyMethod, null, addAllMethod);
  }

  boolean hasBuilder() {
    return addMethod != null;
  }
}
