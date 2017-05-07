package net.autobuilder.examples;

import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class PackagePinranhaTest {

  @Test
  public void testAccess() throws Exception {
    String classModifiers = Modifier.toString(
        PackagePinranha_Builder.class.getModifiers());
    assertThat(classModifiers, not(containsString("public")));
    assertThat(classModifiers, containsString("abstract"));
    String builderMethodModifiers = Modifier.toString(
        PackagePinranha_Builder.class.getDeclaredMethod("builder").getModifiers());
    assertThat(builderMethodModifiers, not(containsString("public")));
    String toBuilderMethodModifiers = Modifier.toString(
        PackagePinranha_Builder.class.getDeclaredMethod("builder", PackagePinranha.class)
            .getModifiers());
    assertThat(toBuilderMethodModifiers, not(containsString("public")));
    String factoryMethodModifiers = Modifier.toString(
        PackagePinranha_Builder.class.getDeclaredMethod("perThreadFactory")
            .getModifiers());
    assertThat(factoryMethodModifiers, not(containsString("public")));
    String setterMethodModifiers = Modifier.toString(
        PackagePinranha_Builder.class.getDeclaredMethod("foo", String.class)
            .getModifiers());
    assertThat(setterMethodModifiers, not(containsString("public")));
    assertThat(setterMethodModifiers, containsString("final"));
    String buildMethodModifiers = Modifier.toString(
        PackagePinranha_Builder.class.getDeclaredMethod("build")
            .getModifiers());
    assertThat(buildMethodModifiers, not(containsString("public")));
  }
}