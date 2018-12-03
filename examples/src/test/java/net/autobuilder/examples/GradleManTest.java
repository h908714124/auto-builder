package net.autobuilder.examples;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradleManTest {

  @Test
  void builderTest() {
    GradleMan batman = GradleMan_Builder.builder()
        .good(true)
        .nice(true)
        .snake("snake")
        .build();
    GradleMan badman = GradleMan_Builder.builder(batman)
        .name("Bad")
        .legs(2)
        .good(false)
        .nice(false)
        .snake("fake")
        .build();

    assertEquals(Optional.empty(), batman.getName());
    assertTrue(batman.good());
    assertTrue(batman.isNice());
    assertEquals(OptionalInt.empty(), batman.legs());
    assertEquals("snake", batman.getSnake());
    assertEquals(badman.getName(), Optional.of("Bad"));
    assertFalse(badman.good());
    assertFalse(badman.isNice());
    assertEquals(OptionalInt.of(2), badman.legs());
    assertEquals("fake", badman.getSnake());
  }
}