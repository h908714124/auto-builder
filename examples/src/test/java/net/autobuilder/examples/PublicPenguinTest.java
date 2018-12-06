package net.autobuilder.examples;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicPenguinTest {

  @Test
  void testAccess() throws Exception {
    String classModifiers = Modifier.toString(
        PublicPenguin_Builder.class.getModifiers());
    assertTrue(classModifiers.contains("public"));
    assertTrue(classModifiers.contains("final"));
    String builderMethodModifiers = Modifier.toString(
        PublicPenguin_Builder.class.getDeclaredMethod("builder").getModifiers());
    assertFalse(builderMethodModifiers.contains("public"));
    String toBuilderMethodModifiers = Modifier.toString(
        PublicPenguin_Builder.class.getDeclaredMethod("builder", PublicPenguin.class)
            .getModifiers());
    assertFalse(toBuilderMethodModifiers.contains("public"));
    String setterMethodModifiers = Modifier.toString(
        PublicPenguin_Builder.class.getDeclaredMethod("foo", String.class)
            .getModifiers());
    assertTrue(setterMethodModifiers.contains("public"));
    assertTrue(setterMethodModifiers.contains("final"));
    String buildMethodModifiers = Modifier.toString(
        PublicPenguin_Builder.class.getDeclaredMethod("build")
            .getModifiers());
    assertTrue(buildMethodModifiers.contains("public"));
  }

  @Test
  void testOptionalNull() {
    String nobody = null;
    PublicPenguin p0 = PublicPenguin_Builder.builder().foo("").bar(1).build();
    PublicPenguin p1 = p0.toBuilder().friend("steven").build();
    PublicPenguin p2 = p1.toBuilder().friend(nobody).build();
    assertEquals(Optional.empty(), p0.friend());
    assertEquals(Optional.of("steven"), p1.friend());
    assertEquals(Optional.empty(), p2.friend());
    assertEquals(OptionalInt.of(1), p2.bar());
  }
}