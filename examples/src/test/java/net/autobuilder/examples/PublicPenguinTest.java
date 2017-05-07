package net.autobuilder.examples;

import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class PublicPenguinTest {

  @Test
  public void testAccess() throws Exception {
    String classModifiers = Modifier.toString(
        PublicPenguin_Builder.class.getModifiers());
    assertThat(classModifiers, containsString("public"));
    assertThat(classModifiers, containsString("abstract"));
    String builderMethodModifiers = Modifier.toString(
        PublicPenguin_Builder.class.getDeclaredMethod("builder").getModifiers());
    assertThat(builderMethodModifiers, not(containsString("public")));
    String toBuilderMethodModifiers = Modifier.toString(
        PublicPenguin_Builder.class.getDeclaredMethod("builder", PublicPenguin.class)
            .getModifiers());
    // the static methods are never public
    assertThat(toBuilderMethodModifiers, not(containsString("public")));
    String factoryMethodModifiers = Modifier.toString(
        PublicPenguin_Builder.class.getDeclaredMethod("perThreadFactory")
            .getModifiers());
    assertThat(factoryMethodModifiers, not(containsString("public")));
    String setterMethodModifiers = Modifier.toString(
        PublicPenguin_Builder.class.getDeclaredMethod("foo", String.class)
            .getModifiers());
    assertThat(setterMethodModifiers, containsString("public"));
    assertThat(setterMethodModifiers, containsString("final"));
    String buildMethodModifiers = Modifier.toString(
        PublicPenguin_Builder.class.getDeclaredMethod("build")
            .getModifiers());
    assertThat(buildMethodModifiers, containsString("public"));
  }
}