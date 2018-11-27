package net.autobuilder.examples;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BirdTest {

  private final List<String> nothing = null;

  @Test
  void testBird() {
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
    assertEquals(0, bird.beak().size());
    assertEquals(0, bird.eyes().size());
    assertEquals(0, bird.feathers().size());
    assertEquals(0, bird.feet().size());
    assertEquals(0, bird.tail().size());
    assertEquals(0, bird.wings().size());
    assertEquals(3, bard.beak().size());
    assertEquals(1, bard.eyes().size());
    assertEquals(1, bard.feathers().size());
    assertEquals(1, bard.feet().size());
    assertEquals(3, bard.tail().size());
    assertEquals(3, bard.wings().size());
    assertEquals(3, bord.beak().size());
    assertEquals(1, bord.eyes().size());
    assertEquals(1, bord.feathers().size());
    assertEquals(1, bord.feet().size());
    assertEquals(3, bord.tail().size());
    assertEquals(3, bord.wings().size());
  }

  @Test
  void testAccumulate() {
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
    assertEquals(0, bird.beak().size());
    assertEquals(0, bird.eyes().size());
    assertEquals(0, bird.feathers().size());
    assertEquals(0, bird.feet().size());
    assertEquals(0, bird.tail().size());
    assertEquals(0, bird.wings().size());
    assertEquals(0, bard.beak().size());
    assertEquals(1, bard.eyes().size());
    assertEquals(1, bard.feathers().size());
    assertEquals(1, bard.feet().size());
    assertEquals(0, bard.tail().size());
    assertEquals(0, bard.wings().size());
    assertEquals(0, bord.beak().size());
    assertEquals(4, bord.eyes().size());
    assertEquals(2, bord.feathers().size());
    assertEquals(1, bord.feet().size());
    assertEquals(0, bord.tail().size());
    assertEquals(0, bord.wings().size());
    assertEquals(1, burd.eyes().size());
    assertEquals(1, burd.tail().size());
  }

  @Test
  void testBirdNoCache() {
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
    assertEquals(0, bird.beak().size());
    assertEquals(0, bird.eyes().size());
    assertEquals(0, bird.feathers().size());
    assertEquals(0, bird.feet().size());
    assertEquals(0, bird.tail().size());
    assertEquals(0, bird.wings().size());
    assertEquals(1, bard.beak().size());
    assertEquals(1, bard.eyes().size());
    assertEquals(1, bard.feathers().size());
    assertEquals(1, bard.feet().size());
    assertEquals(1, bard.tail().size());
    assertEquals(1, bard.wings().size());
    assertEquals(1, bord.beak().size());
    assertEquals(1, bord.eyes().size());
    assertEquals(1, bord.feathers().size());
    assertEquals(1, bord.feet().size());
    assertEquals(1, bord.tail().size());
    assertEquals(1, bord.wings().size());
  }

  @Test
  void testAccumulateNoCache() {
    Bird bird = Bird_Builder.builder().build();
    Bird bard = Bird_Builder.builder(bird)
        .putInEyes("", "")
        .addToFeathers(singletonList(new Date()))
        .addToFeet("")
        .build();
    Bird bord = Bird_Builder.builder(bard)
        .addToFeathers(new Date())
        .build();
    assertEquals(0, bird.beak().size());
    assertEquals(0, bird.eyes().size());
    assertEquals(0, bird.feathers().size());
    assertEquals(0, bird.feet().size());
    assertEquals(0, bird.tail().size());
    assertEquals(0, bird.wings().size());
    assertEquals(0, bard.beak().size());
    assertEquals(1, bard.eyes().size());
    assertEquals(1, bard.feathers().size());
    assertEquals(1, bard.feet().size());
    assertEquals(0, bard.tail().size());
    assertEquals(0, bard.wings().size());
    assertEquals(0, bord.beak().size());
    assertEquals(1, bord.eyes().size());
    assertEquals(2, bord.feathers().size());
    assertEquals(1, bord.feet().size());
    assertEquals(0, bord.tail().size());
    assertEquals(0, bord.wings().size());
  }

  @Test
  void testNest() {
    Bird.Nest nest = Bird_Nest_Builder.builder().addToSticks("").build();
    Bird.Nest test = Bird_Nest_Builder.builder(nest)
        .feathers(ImmutableList.of(ImmutableList.of("")))
        .build();
    Bird.Nest best = Bird_Nest_Builder.builder(test)
        .addToSticks("best")
        .build();
    assertEquals(1, test.feathers().size());
    assertEquals(1, best.feathers().size());
    assertEquals("best", best.addToSticks());
  }
}