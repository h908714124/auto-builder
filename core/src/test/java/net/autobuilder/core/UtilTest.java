package net.autobuilder.core;

import org.junit.Test;

import java.util.stream.Stream;

import static net.autobuilder.core.Util.isDistinct;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class UtilTest {
  @Test
  public void distinct() throws Exception {
    assertThat(Stream.empty().collect(isDistinct()), is(true));
    assertThat(Stream.of(1).collect(isDistinct()), is(true));
    assertThat(Stream.of(1, 2).collect(isDistinct()), is(true));
    assertThat(Stream.of(1, 1).collect(isDistinct()), is(false));
  }
}