package net.autobuilder.examples;

import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AnimalTest {

  @Test
  public void testBasic() throws Exception {
    Animal spiderPig = Animal_Builder.builder()
        .name("Spider-Pig")
        .maybe("yo")
        .maybeMaybe(Optional.of(Optional.of("mama")))
        .good(false)
        .build();
    Animal horse = spiderPig.toBuilder()
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

  @Test
  public void testFactoryNestingWorksCorrectly() throws Exception {
    Animal spiderPig = Animal_Builder.builder().name("").build();
    Animal horse = spiderPig.toBuilder()
        .good(true)
        .name(spiderPig.toBuilder()
            .name("Horse")
            .good(false)
            .build().getName())
        .build();
    assertThat(horse.getName(), is("Horse"));
    assertThat("nested builder calls leads to incorrect results",
        horse.isGood(), is(true));
  }

  @Test
  public void testFactoryBuildersAreReused() throws Exception {
    Animal spiderPig = Animal_Builder.builder().name("").build();
    Animal_Builder builder_1 = spiderPig.toBuilder();
    Animal badger = builder_1.name("Badger").build();
    Animal_Builder builder_2 = spiderPig.toBuilder();
    Animal snake = builder_2.name("Snake").build();
    assertThat(badger.getName(), is("Badger"));
    assertThat(snake.getName(), is("Snake"));
    assertThat("builders are not reused",
        builder_1, sameInstance(builder_2));
  }
}