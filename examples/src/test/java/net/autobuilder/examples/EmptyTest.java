package net.autobuilder.examples;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EmptyTest {

  @Test
  public void testEmpty() {
    Empty e1 = Empty_Builder.builder().build();
    Empty e2 = Empty_Builder.builder(e1).build();
    assertThat(e1, is(e2));
  }
}