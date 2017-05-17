package net.autobuilder.examples;

import org.junit.Test;

import java.util.Optional;
import java.util.OptionalInt;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class GradleManTest {

  @Test
  public void builderTest() throws Exception {
    GradleMan<String> batman = GradleMan_Builder.<String>builder()
        .good(true)
        .nice(true)
        .snake("snake")
        .build();
    GradleMan<String> badman = GradleMan_Builder.builder(batman)
        .name("Bad")
        .legs(2)
        .good(false)
        .nice(false)
        .snake("fake")
        .build();

    assertThat(batman.getName(), is(Optional.empty()));
    assertThat(batman.good(), is(true));
    assertThat(batman.isNice(), is(true));
    assertThat(batman.legs(), is(OptionalInt.empty()));
    assertThat(batman.getSnake(), is("snake"));

    assertThat(badman.getName(), is(Optional.of("Bad")));
    assertThat(badman.good(), is(false));
    assertThat(badman.isNice(), is(false));
    assertThat(badman.legs(), is(OptionalInt.of(2)));
    assertThat(badman.getSnake(), is("fake"));
  }
}