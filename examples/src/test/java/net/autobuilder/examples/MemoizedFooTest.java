package net.autobuilder.examples;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoizedFooTest {

  @Test
  void testMemoized() {
    MemoizedFoo foo = MemoizedFoo_Builder.builder()
        .barProperty("123")
        .zarProperty(true)
        .build();
    assertTrue(foo.zarProperty());
    assertEquals("123", foo.barProperty());
    assertEquals("your 123 is ready", foo.derivedProperty());
    assertEquals("your true is ready", foo.derivedProperty2());
  }
}
