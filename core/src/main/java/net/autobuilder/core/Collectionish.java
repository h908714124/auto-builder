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
      of("com.google.common.collect", "ImmutableList", "of"),
      of("com.google.common.collect", "ImmutableSet", "of"),
      of("com.google.common.collect", "ImmutableMap", "of"));

  private final ClassName className;
  final ClassName factoryClassName;
  final String emptyMethod;

  private Collectionish(ClassName className, ClassName factoryClassName, String emptyMethod) {
    this.className = className;
    this.factoryClassName = factoryClassName;
    this.emptyMethod = emptyMethod;
  }

  private static Collectionish of(Class<?> className, Class<?> factoryClassName, String emptyMethod) {
    return new Collectionish(ClassName.get(className), ClassName.get(factoryClassName), emptyMethod);
  }

  private static Collectionish of(String packageName, String simpleName, String emptyMethod) {
    ClassName className = ClassName.get(packageName, simpleName);
    return new Collectionish(className, className, emptyMethod);
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

}
