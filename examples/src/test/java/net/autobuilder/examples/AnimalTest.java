package net.autobuilder.examples;

import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AnimalTest {

  @Test
  public void name() throws Exception {
    Animal spiderPig = Animal_Builder.builder()
        .name("Spider-Pig")
        .maybe("yo")
        .maybeMaybe(Optional.of(Optional.of("mama")))
        .good(false)
        .build();
    Animal horse = Animal_Builder.builder(spiderPig)
        .name("Horse")
        .good(true)
        .build();
    assertThat(spiderPig.getName(), is("Spider-Pig"));
    assertThat(spiderPig.isGood(), is(false));
    assertThat(spiderPig.maybe(), is(Optional.of("yo")));
    assertThat(spiderPig.maybeMaybe(), is(Optional.of(Optional.of("mama"))));
    assertThat(horse.getName(), is("Horse"));
    assertThat(horse.isGood(), is(true));
    assertThat(horse.maybe(), is(Optional.of("yo")));
    assertThat(horse.maybeMaybe(), is(Optional.of(Optional.of("mama"))));
  }
}