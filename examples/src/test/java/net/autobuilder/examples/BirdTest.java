package net.autobuilder.examples;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class BirdTest {

  @Test
  public void testNest() throws Exception {
    Bird.Nest test = Bird_Nest_Builder.builder()
        .name("test")
        .build();
    Bird.Nest best = Bird_Nest_Builder.builder(test)
        .name("best")
        .build();
    assertThat(test.name(), is("test"));
    assertThat(best.name(), is("best"));
  }
}