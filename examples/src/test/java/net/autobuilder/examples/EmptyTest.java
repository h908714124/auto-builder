package net.autobuilder.examples;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmptyTest {

  @Test
  void testEmpty() {
    Empty e1 = Empty_Builder.builder().build();
    Empty e2 = Empty_Builder.builder(e1).build();
    assertEquals(e2, e1);
  }
}