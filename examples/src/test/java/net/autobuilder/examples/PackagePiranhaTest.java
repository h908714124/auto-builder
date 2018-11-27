package net.autobuilder.examples;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackagePiranhaTest {

  @Test
  void testAccess() throws Exception {
    String classModifiers = Modifier.toString(
        PackagePiranha_Builder.class.getModifiers());
    assertFalse(classModifiers.contains("public"));
    assertTrue(classModifiers.contains("abstract"));
    String builderMethodModifiers = Modifier.toString(
        PackagePiranha_Builder.class.getDeclaredMethod("builder").getModifiers());
    assertFalse(builderMethodModifiers.contains("public"));
    String toBuilderMethodModifiers = Modifier.toString(
        PackagePiranha_Builder.class.getDeclaredMethod("builder", PackagePiranha.class)
            .getModifiers());
    assertFalse(toBuilderMethodModifiers.contains("public"));
    String factoryMethodModifiers = Modifier.toString(
        PackagePiranha_Builder.class.getDeclaredMethod("perThreadFactory")
            .getModifiers());
    assertFalse(factoryMethodModifiers.contains("public"));
    String setterMethodModifiers = Modifier.toString(
        PackagePiranha_Builder.class.getDeclaredMethod("foo", String.class)
            .getModifiers());
    assertFalse(setterMethodModifiers.contains("public"));
    assertTrue(setterMethodModifiers.contains("final"));
    String buildMethodModifiers = Modifier.toString(
        PackagePiranha_Builder.class.getDeclaredMethod("build")
            .getModifiers());
    assertFalse(buildMethodModifiers.contains("public"));
  }
}