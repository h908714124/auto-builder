package net.autobuilder.core;

import com.squareup.javapoet.TypeName;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ParameterTest {

  @Test
  public void setterName() throws Exception {
    assertThat(Parameter.setterName("foo", TypeName.LONG), is("foo"));
    assertThat(Parameter.setterName("getFoo", TypeName.LONG), is("foo"));
    assertThat(Parameter.setterName("isBar", TypeName.BOOLEAN), is("bar"));
  }
}