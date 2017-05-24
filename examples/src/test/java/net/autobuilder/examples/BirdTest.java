package net.autobuilder.examples;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class BirdTest {

  private final List<String> nothing = null;

  @Test
  public void testBird() throws Exception {
    Map<Date, String> map1 = new HashMap<>();
    Map<Date, String> map2 = new HashMap<>();
    HashSet<String> set1 = new HashSet<>();
    set1.add("");
    map1.put(new Date(1), "");
    map2.put(new Date(2), "");
    Bird bird = Bird_Builder.builder().build();
    Bird bard = bird.toBuilder()
        .beak(singletonList(new Date()))
        .wings(set1)
        .addToBeak(singletonList(new Date()))
        .addToBeak(new Date())
        .addToWings("ふぐ")
        .addToWings(nothing)
        .addToWings(singletonList("魚"))
        .eyes(ImmutableMap.of("", ""))
        .putInEyes(null)
        .feathers(singletonList(new Date()))
        .feet(ImmutableSet.of(""))
        .addToFeet(nothing)
        .tail(map1)
        .putInTail(new Date(0), "")
        .putInTail(map2)
        .putInTail(null)
        .build();
    Bird bord = bard.toBuilder().build();
    assertThat(bird.beak().size(), is(0));
    assertThat(bird.eyes().size(), is(0));
    assertThat(bird.feathers().size(), is(0));
    assertThat(bird.feet().size(), is(0));
    assertThat(bird.tail().size(), is(0));
    assertThat(bird.wings().size(), is(0));
    assertThat(bard.beak().size(), is(3));
    assertThat(bard.eyes().size(), is(1));
    assertThat(bard.feathers().size(), is(1));
    assertThat(bard.feet().size(), is(1));
    assertThat(bard.tail().size(), is(3));
    assertThat(bard.wings().size(), is(3));
    assertThat(bord.beak().size(), is(3));
    assertThat(bord.eyes().size(), is(1));
    assertThat(bord.feathers().size(), is(1));
    assertThat(bord.feet().size(), is(1));
    assertThat(bord.tail().size(), is(3));
    assertThat(bord.wings().size(), is(3));
  }

  @Test
  public void testAccumulate() {
    Bird bird = Bird_Builder.builder().build();
    Bird bard = bird.toBuilder()
        .putInEyes("", "")
        .addToFeathers(new Date())
        .addToFeet(nothing)
        .addToFeet("")
        .build();
    Bird bord = bard.toBuilder()
        .putInEyes(" ", "")
        .putInEyes(ImmutableMap.of("  ", "", "   ", "").entrySet())
        .putInEyes(null)
        .addToFeathers(new Date())
        .build();
    Bird burd = bord.toBuilder()
        .eyes(null)
        .putInEyes("", "")
        .eyes(ImmutableMap.of())
        .putInEyes("", "")
        .tail(null)
        .putInTail(new Date(1), "")
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
    assertThat(burd.tail().size(), is(1));
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