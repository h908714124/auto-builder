package net.autobuilder.examples;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class BirdTest {
  @Test
  public void testBird() throws Exception {
    Map<String, String> map1 = new HashMap<>();
    HashSet<String> set1 = new HashSet<>();
    set1.add("");
    map1.put("", "");
    Bird bird = Bird_Builder.builder().build();
    Bird bard = Bird_Builder.builder(bird)
        .beak(Collections.singletonList(""))
        .eyes(ImmutableMap.of("", ""))
        .feathers(ImmutableList.of(""))
        .feet(ImmutableSet.of(""))
        .tail(map1)
        .wings(set1)
        .build();
    assertThat(bird.beak().size(), is(0));
    assertThat(bird.eyes().size(), is(0));
    assertThat(bird.feathers().size(), is(0));
    assertThat(bird.feet().size(), is(0));
    assertThat(bird.tail().size(), is(0));
    assertThat(bird.wings().size(), is(0));
    assertThat(bard.beak().size(), is(1));
    assertThat(bard.eyes().size(), is(1));
    assertThat(bard.feathers().size(), is(1));
    assertThat(bard.feet().size(), is(1));
    assertThat(bard.tail().size(), is(1));
    assertThat(bard.wings().size(), is(1));
  }

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