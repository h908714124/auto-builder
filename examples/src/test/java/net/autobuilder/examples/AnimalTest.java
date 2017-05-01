package net.autobuilder.examples;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AnimalTest {

  @Test
  public void name() throws Exception {
    Animal spiderPig = Animal_Builder.builder()
        .name("Spider-Pig")
        .build();
    Animal horse = Animal_Builder.builder(spiderPig)
        .name("Horse")
        .build();
    assertThat(spiderPig.name(), is("Spider-Pig"));
    assertThat(horse.name(), is("Horse"));
  }
}