package net.autobuilder.core;

import com.google.auto.value.processor.AutoValueProcessor;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static java.util.Collections.singletonList;

class PostBuildTest {

  @Test
  void postBuildReturnsInt() {

    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.autobuilder.AutoBuilder;",
        "import net.autobuilder.PostBuild;",
        "import com.google.auto.value.AutoValue;",
        "import java.util.List;",
        "",
        "@AutoBuilder",
        "@AutoValue",
        "abstract class Animal {",
        "  abstract List<String> blub();",
        "  @PostBuild",
        "  int done() { return 1; }",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Animal", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new AutoBuilderProcessor(), new AutoValueProcessor())
        .compilesWithoutError();
  }

  @Test
  void postBuildReturnsVoid() {

    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.autobuilder.AutoBuilder;",
        "import net.autobuilder.PostBuild;",
        "import com.google.auto.value.AutoValue;",
        "import java.util.List;",
        "",
        "@AutoBuilder",
        "@AutoValue",
        "abstract class Animal {",
        "  abstract List<String> blub();",
        "  @PostBuild",
        "  void done() { }",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Animal", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new AutoBuilderProcessor(), new AutoValueProcessor())
        .compilesWithoutError();
  }

  @Test
  void twoPostBuildMethods() {

    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.autobuilder.AutoBuilder;",
        "import net.autobuilder.PostBuild;",
        "import com.google.auto.value.AutoValue;",
        "import java.util.List;",
        "",
        "@AutoBuilder",
        "@AutoValue",
        "abstract class Animal {",
        "  abstract List<String> blub();",
        "  @PostBuild",
        "  void done() { }",
        "  @PostBuild",
        "  void kapow() { }",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Animal", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new AutoBuilderProcessor(), new AutoValueProcessor())
        .failsToCompile()
        .withErrorContaining("Only one method can have the PostBuild annotation.");
  }

  @Test
  void invalidBuildMethodWithParameter() {

    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.autobuilder.AutoBuilder;",
        "import net.autobuilder.PostBuild;",
        "import com.google.auto.value.AutoValue;",
        "import java.util.List;",
        "",
        "@AutoBuilder",
        "@AutoValue",
        "abstract class Animal {",
        "  abstract List<String> blub();",
        "  @PostBuild",
        "  void done() { }",
        "  @PostBuild",
        "  void kapow(int x) { }",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Animal", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new AutoBuilderProcessor(), new AutoValueProcessor())
        .failsToCompile()
        .withErrorContaining("The method may not have any parameters.");
  }
}
