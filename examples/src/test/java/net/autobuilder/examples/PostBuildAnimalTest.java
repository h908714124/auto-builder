package net.autobuilder.examples;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostBuildAnimalTest {

  @Test
  void testPostBuild() throws IOException {
    PostBuildAnimal animal = PostBuildAnimal_Builder.builder().snake("snape").build();
    assertEquals("snape", animal.getSnake());
  }

  @Test
  void testPostBuildError() {
    IOException exception = assertThrows(IOException.class,
        () -> PostBuildAnimal_Builder.builder().snake("").build());
    assertEquals("missing snake", exception.getMessage());
  }
}
