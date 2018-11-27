package net.autobuilder.core;

import com.squareup.javapoet.TypeName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParameterTest {

  @Test
  void setterName() {
    assertEquals("foo", Parameter.setterName("foo", TypeName.LONG));
    assertEquals("foo", Parameter.setterName("getFoo", TypeName.LONG));
    assertEquals("bar", Parameter.setterName("isBar", TypeName.BOOLEAN));
  }
}