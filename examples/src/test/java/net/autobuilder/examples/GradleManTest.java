package net.autobuilder.examples;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class GradleManTest {

  @Test
  public void builderTest() throws Exception {
    GradleMan<String> batman = GradleMan.create("Uwe", true);
    GradleMan<String> badman = GradleMan_Builder.builder(batman)
        .good(false)
        .build();
    assertThat(batman.isGood(), is(true));
    assertThat(badman.isGood(), is(false));
  }
}