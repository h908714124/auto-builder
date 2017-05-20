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
    Bird bard = bird.toBuilder()
        .beak(Collections.singletonList(""))
        .eyes(ImmutableMap.of("", ""))
        .feathers(ImmutableList.of(""))
        .feet(ImmutableSet.of(""))
        .tail(map1)
        .wings(set1)
        .build();
    Bird bord = bard.toBuilder().build();
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
    assertThat(bord.beak().size(), is(1));
    assertThat(bord.eyes().size(), is(1));
    assertThat(bord.feathers().size(), is(1));
    assertThat(bord.feet().size(), is(1));
    assertThat(bord.tail().size(), is(1));
    assertThat(bord.wings().size(), is(1));
  }

  @Test
  public void testAggregate() {
    Bird bird = Bird_Builder.builder().build();
    Bird bard = bird.toBuilder()
        .putInEyes("", "")
        .addToFeathers("")
        .addToFeet("")
        .build();
    Bird bord = bard.toBuilder()
        .putInEyes(" ", "")
        .addToFeathers("")
        .build();
    Bird burd = bord.toBuilder()
        .eyes(null)
        .putInEyes("", "")
        .eyes(ImmutableMap.of())
        .putInEyes("", "")
        .build();
    assertThat(bird.beak().size(), is(0));
    assertThat(bird.eyes().size(), is(0));
    assertThat(bird.feathers().size(), is(0));
    assertThat(bird.feet().size(), is(0));
    assertThat(bird.tail().size(), is(0));
    assertThat(bird.wings().size(), is(0));
    assertThat(bard.beak().size(), is(0));
    assertThat(bard.eyes().size(), is(1));
    assertThat(bard.feathers().size(), is(1));
    assertThat(bard.feet().size(), is(1));
    assertThat(bard.tail().size(), is(0));
    assertThat(bard.wings().size(), is(0));
    assertThat(bord.beak().size(), is(0));
    assertThat(bord.eyes().size(), is(2));
    assertThat(bord.feathers().size(), is(2));
    assertThat(bord.feet().size(), is(1));
    assertThat(bord.tail().size(), is(0));
    assertThat(bord.wings().size(), is(0));
    assertThat(burd.eyes().size(), is(1));
  }

  @Test
  public void testBirdNoCache() throws Exception {
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
    Bird bord = Bird_Builder.builder(bard).build();
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
    assertThat(bord.beak().size(), is(1));
    assertThat(bord.eyes().size(), is(1));
    assertThat(bord.feathers().size(), is(1));
    assertThat(bord.feet().size(), is(1));
    assertThat(bord.tail().size(), is(1));
    assertThat(bord.wings().size(), is(1));
  }

  @Test
  public void testAggregateNoCache() {
    Bird bird = Bird_Builder.builder().build();
    Bird bard = Bird_Builder.builder(bird)
        .putInEyes("", "")
        .addToFeathers("")
        .addToFeet("")
        .build();
    Bird bord = Bird_Builder.builder(bard)
        .addToFeathers("")
        .build();
    assertThat(bird.beak().size(), is(0));
    assertThat(bird.eyes().size(), is(0));
    assertThat(bird.feathers().size(), is(0));
    assertThat(bird.feet().size(), is(0));
    assertThat(bird.tail().size(), is(0));
    assertThat(bird.wings().size(), is(0));
    assertThat(bard.beak().size(), is(0));
    assertThat(bard.eyes().size(), is(1));
    assertThat(bard.feathers().size(), is(1));
    assertThat(bard.feet().size(), is(1));
    assertThat(bard.tail().size(), is(0));
    assertThat(bard.wings().size(), is(0));
    assertThat(bord.beak().size(), is(0));
    assertThat(bord.eyes().size(), is(1));
    assertThat(bord.feathers().size(), is(2));
    assertThat(bord.feet().size(), is(1));
    assertThat(bord.tail().size(), is(0));
    assertThat(bord.wings().size(), is(0));
  }

  @Test
  public void testNest() throws Exception {
    Bird.Nest nest = Bird_Nest_Builder.builder().addToFeathers("").build();
    Bird.Nest test = Bird_Nest_Builder.builder(nest)
        .feathers(ImmutableList.of(""))
        .build();
    Bird.Nest best = Bird_Nest_Builder.builder(test)
        .addToFeathers("best")
        .build();
    assertThat(test.feathers(), is(ImmutableList.of("")));
    assertThat(best.feathers(), is(ImmutableList.of("")));
    assertThat(best.addToFeathers(), is("best"));
  }
}