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

class MemoizedTest {

  @Test
  void testMemoized() {

    List<String> sourceLines = Arrays.asList(
        "package test;",
        "import net.autobuilder.AutoBuilder;",
        "import com.google.auto.value.AutoValue;",
        "import com.google.auto.value.extension.memoized.Memoized;",
        "",
        "@AutoBuilder",
        "@AutoValue",
        "abstract class Foo {",
        "",
        "  abstract String barProperty();",
        "",
        "  @Memoized",
        "  String derivedProperty() {",
        "    return barProperty();",
        "  }",
        "}");
    JavaFileObject javaFile = forSourceLines("test.Foo", sourceLines);
    assertAbout(javaSources()).that(singletonList(javaFile))
        .processedWith(new AutoBuilderProcessor(), new AutoValueProcessor())
        .compilesWithoutError();
  }
}
