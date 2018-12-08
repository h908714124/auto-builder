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

class BoundsTest {

  @Test
  void listOfList1() {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.autobuilder.AutoBuilder;",
        "import com.google.auto.value.AutoValue;",
        "import java.util.List;",
        "",
        "@AutoBuilder",
        "@AutoValue",
        "abstract class Animal {",
        "  abstract List<? extends List<? extends List>> feathers();",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Animal", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new AutoBuilderProcessor(), new AutoValueProcessor())
        .compilesWithoutError();
  }

  @Test
  void listOfList2() {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.autobuilder.AutoBuilder;",
        "import com.google.auto.value.AutoValue;",
        "import java.util.List;",
        "",
        "@AutoBuilder",
        "@AutoValue",
        "abstract class Animal {",
        "  abstract List<List<List>> feathers();",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Animal", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new AutoBuilderProcessor(), new AutoValueProcessor())
        .compilesWithoutError();
  }

  @Test
  void listOfList3() {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.autobuilder.AutoBuilder;",
        "import com.google.auto.value.AutoValue;",
        "import java.util.List;",
        "",
        "@AutoBuilder",
        "@AutoValue",
        "abstract class Animal {",
        "  abstract List<? extends List<List>> feathers();",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Animal", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new AutoBuilderProcessor(), new AutoValueProcessor())
        .compilesWithoutError();
  }

  @Test
  void listOfList4() {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.autobuilder.AutoBuilder;",
        "import com.google.auto.value.AutoValue;",
        "import java.util.List;",
        "",
        "@AutoBuilder",
        "@AutoValue",
        "abstract class Animal {",
        "  abstract List<List<? extends List>> feathers();",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Animal", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new AutoBuilderProcessor(), new AutoValueProcessor())
        .compilesWithoutError();
  }

  @Test
  void listOfList5() {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.autobuilder.AutoBuilder;",
        "import com.google.auto.value.AutoValue;",
        "import java.util.List;",
        "",
        "@AutoBuilder",
        "@AutoValue",
        "abstract class Animal {",
        "  abstract List<List> feathers();",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Animal", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new AutoBuilderProcessor(), new AutoValueProcessor())
        .compilesWithoutError();
  }

  @Test
  void listOfList6() {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.autobuilder.AutoBuilder;",
        "import com.google.auto.value.AutoValue;",
        "import java.util.List;",
        "",
        "@AutoBuilder",
        "@AutoValue",
        "abstract class Animal {",
        "  abstract List feathers();",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Animal", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new AutoBuilderProcessor(), new AutoValueProcessor())
        .compilesWithoutError();
  }

  @Test
  void optionalOfOptional1() {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.autobuilder.AutoBuilder;",
        "import com.google.auto.value.AutoValue;",
        "import java.util.Optional;",
        "",
        "@AutoBuilder",
        "@AutoValue",
        "abstract class Animal {",
        "  abstract Optional<? extends Optional<? extends Optional>> feathers();",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Animal", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new AutoBuilderProcessor(), new AutoValueProcessor())
        .compilesWithoutError();
  }

  @Test
  void optionalOfOptional2() {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.autobuilder.AutoBuilder;",
        "import com.google.auto.value.AutoValue;",
        "import java.util.Optional;",
        "",
        "@AutoBuilder",
        "@AutoValue",
        "abstract class Animal {",
        "  abstract Optional<Optional<Optional>> feathers();",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Animal", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new AutoBuilderProcessor(), new AutoValueProcessor())
        .compilesWithoutError();
  }

  @Test
  void optionalOfOptional3() {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.autobuilder.AutoBuilder;",
        "import com.google.auto.value.AutoValue;",
        "import java.util.Optional;",
        "",
        "@AutoBuilder",
        "@AutoValue",
        "abstract class Animal {",
        "  abstract Optional<? extends Optional<Optional>> feathers();",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Animal", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new AutoBuilderProcessor(), new AutoValueProcessor())
        .compilesWithoutError();
  }

  @Test
  void optionalOfOptional4() {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.autobuilder.AutoBuilder;",
        "import com.google.auto.value.AutoValue;",
        "import java.util.Optional;",
        "",
        "@AutoBuilder",
        "@AutoValue",
        "abstract class Animal {",
        "  abstract Optional<Optional<? extends Optional>> feathers();",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Animal", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new AutoBuilderProcessor(), new AutoValueProcessor())
        .compilesWithoutError();
  }

  @Test
  void optionalOfOptional5() {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.autobuilder.AutoBuilder;",
        "import com.google.auto.value.AutoValue;",
        "import java.util.Optional;",
        "",
        "@AutoBuilder",
        "@AutoValue",
        "abstract class Animal {",
        "  abstract Optional<Optional> feathers();",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Animal", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new AutoBuilderProcessor(), new AutoValueProcessor())
        .compilesWithoutError();
  }

  @Test
  void optionalOfOptional6() {
    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.autobuilder.AutoBuilder;",
        "import com.google.auto.value.AutoValue;",
        "import java.util.Optional;",
        "",
        "@AutoBuilder",
        "@AutoValue",
        "abstract class Animal {",
        "  abstract Optional feathers();",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Animal", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new AutoBuilderProcessor(), new AutoValueProcessor())
        .compilesWithoutError();
  }
}
