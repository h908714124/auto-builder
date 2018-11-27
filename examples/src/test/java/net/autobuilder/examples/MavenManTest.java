package net.autobuilder.examples;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MavenManTest {

  @Test
  void builderTest() {
    MavenMan batman = MavenMan.create("Batman", true);
    MavenMan robin = MavenMan_Builder.builder(batman)
        .name("Robin")
        .build();
    assertEquals("Batman", batman.name());
    assertEquals("Robin", robin.name());
  }
}