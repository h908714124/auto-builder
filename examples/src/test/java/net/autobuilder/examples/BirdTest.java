package net.autobuilder.examples;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class BirdTest {

  @Test
  public void testBird() throws Exception {
    Map<Date, String> map1 = new HashMap<>();
    HashSet<String> set1 = new HashSet<>();
    set1.add("");
    map1.put(new Date(), "");
    Bird bird = Bird_Builder.builder().build();
    Bird bard = bird.toBuilder()
        .beak(singletonList(new Date()))
        .eyes(ImmutableMap.of("", ""))
        .feathers(singletonList(new Date()))
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
  public void testAccumulate() {
    Bird bird = Bird_Builder.builder().build();
    Bird bard = bird.toBuilder()
        .putInEyes("", "")
        .addToFeathers(new Date())
        .addToFeet("")
        .build();
    Bird bord = bard.toBuilder()
        .putInEyes(" ", "")
        .putInEyes(ImmutableMap.of("  ", "", "   ", "").entrySet())
        .addToFeathers(new Date())
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
    assertThat(bord.eyes().size(), is(4));
    assertThat(bord.feathers().size(), is(2));
    assertThat(bord.feet().size(), is(1));
    assertThat(bord.tail().size(), is(0));
    assertThat(bord.wings().size(), is(0));
    assertThat(burd.eyes().size(), is(1));
  }

  @Test
  public void testBirdNoCache() throws Exception {
    Map<Date, String> map1 = new HashMap<>();
    HashSet<String> set1 = new HashSet<>();
    set1.add("");
    map1.put(new Date(), "");
    Bird bird = Bird_Builder.builder().build();
    Bird bard = Bird_Builder.builder(bird)
        .beak(singletonList(new Date()))
        .eyes(ImmutableMap.of("", ""))
        .feathers(ImmutableList.of(new Date()))
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
  public void testAccumulateNoCache() {
    Bird bird = Bird_Builder.builder().build();
    Bird bard = Bird_Builder.builder(bird)
        .putInEyes("", "")
        .addToFeathers(singletonList(new Date()))
        .addToFeet("")
        .build();
    Bird bord = Bird_Builder.builder(bard)
        .addToFeathers(new Date())
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
    Bird.Nest nest = Bird_Nest_Builder.builder().addToSticks("").build();
    Bird.Nest test = Bird_Nest_Builder.builder(nest)
        .feathers(ImmutableList.of(ImmutableList.of("")))
        .build();
    Bird.Nest best = Bird_Nest_Builder.builder(test)
        .addToSticks("best")
        .build();
    assertThat(test.feathers().size(), is(1));
    assertThat(best.feathers().size(), is(1));
    assertThat(best.addToSticks(), is("best"));
  }
}