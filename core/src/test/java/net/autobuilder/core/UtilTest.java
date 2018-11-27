package net.autobuilder.core;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static net.autobuilder.core.Util.isDistinct;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilTest {

  @Test
  void distinct() {
    assertTrue(Stream.empty().collect(isDistinct()));
    assertTrue(Stream.of(1).collect(isDistinct()));
    assertTrue(Stream.of(1, 2).collect(isDistinct()));
    assertFalse(Stream.of(1, 1).collect(isDistinct()));
  }
}