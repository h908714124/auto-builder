package net.autobuilder.core;

import com.google.auto.value.processor.AutoValueProcessor;
import org.junit.Test;

import javax.tools.JavaFileObject;
import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static java.util.Collections.singletonList;

public class ProcessorTest {

  @Test
  public void autoBuilderFirst() throws Exception {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.autobuilder.AutoBuilder;",
        "import com.google.auto.value.AutoValue;",
        "",
        "@AutoBuilder",
        "@AutoValue",
        "abstract class Animal {",
        "  abstract String foo();",
        "  final Animal_Builder toBuilder() {",
        "    return Animal_Builder.builder(this);",
        "  }",
        "}");
    JavaFileObject javaFile = forSourceLines("test.JJobParser", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new AutoBuilderProcessor(), new AutoValueProcessor())
        .compilesWithoutError();
  }

  @Test
  public void autoValueFirst() throws Exception {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.autobuilder.AutoBuilder;",
        "import com.google.auto.value.AutoValue;",
        "",
        "@AutoBuilder",
        "@AutoValue",
        "abstract class Animal {",
        "  abstract String foo();",
        "  final Animal_Builder toBuilder() {",
        "    return Animal_Builder.builder(this);",
        "  }",
        "}");
    JavaFileObject javaFile = forSourceLines("test.JJobParser", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new AutoValueProcessor(), new AutoBuilderProcessor())
        .compilesWithoutError();
  }
}