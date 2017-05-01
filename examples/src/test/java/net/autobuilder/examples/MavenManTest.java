package net.autobuilder.examples;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class MavenManTest {

  @Test
  public void builderTest() throws Exception {
    MavenMan batman = MavenMan.create("Batman", true);
    MavenMan robin = MavenMan_Builder.builder(batman)
        .name("Robin")
        .build();
    assertThat(batman.name(), is("Batman"));
    assertThat(robin.name(), is("Robin"));
  }
}