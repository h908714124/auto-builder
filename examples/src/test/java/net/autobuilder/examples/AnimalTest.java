package net.autobuilder.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimalTest {

  @Test
  void testBasic() {
    Animal spiderPig = Animal_Builder.builder()
        .name("Spider-Pig")
        .maybe("yo")
        .maybeMaybe(Optional.of(Optional.of("mama")))
        .good(false)
        .build();
    Animal horse = spiderPig.toBuilder()
        .name("Horse")
        .good(true)
        .build();
    assertEquals("Spider-Pig", spiderPig.getName());
    assertFalse(spiderPig.isGood());
    assertEquals(Optional.of("yo"), spiderPig.maybe());
    assertEquals(Optional.of(Optional.of("mama")), spiderPig.maybeMaybe());
    assertEquals("Horse", horse.getName());
    assertTrue(horse.isGood());
    assertEquals(Optional.of("yo"), horse.maybe());
    assertEquals(Optional.of(Optional.of("mama")), horse.maybeMaybe());
  }

  @Test
  void testFactoryNestingWorksCorrectly() {
    Animal spiderPig = Animal_Builder.builder().name("").build();
    Animal horse = spiderPig.toBuilder()
        .good(true)
        .name(spiderPig.toBuilder()
            .name("Horse")
            .good(false)
            .build().getName())
        .build();
    assertEquals("Horse", horse.getName());
    assertTrue(horse.isGood(),
        "nested builder calls leads to incorrect results");
  }

  @Test
  void testFactoryBuildersAreReused() {
    Animal spiderPig = Animal_Builder.builder().name("").build();
    Animal_Builder builder_1 = spiderPig.toBuilder();
    Animal badger = builder_1.name("Badger").build();
    Animal_Builder builder_2 = spiderPig.toBuilder();
    Animal snake = builder_2.name("Snake").build();
    assertEquals("Badger", badger.getName());
    assertEquals("Snake", snake.getName());
    Assertions.assertSame(builder_1, builder_2,
        "builders are not reused");
  }
}