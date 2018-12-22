### auto-builder

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.h908714124/auto-builder/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.h908714124/auto-builder)

The builder pattern is useful because it lets us "update" immutable objects.
However the pattern is very repetitive to write and maintain,
so it might be a good idea to actually generate it.

Please note that this processor depends on another processor
called [auto-value](https://github.com/google/auto/tree/master/value).
See below for the necessary configuration.

#### Quick start

Take the `Animal` class from the 
[auto-value docs](https://github.com/google/auto/blob/master/value/userguide/builders.md),
and add the auto-builder annotation:

````java
@AutoBuilder
@AutoValue
abstract class Animal {
  abstract String name();
  abstract int numberOfLegs();

}
````

A class `Animal_Builder` will be generated in the same package as `Animal`.
An instance of `Animal_Builder` can be obtained in two different ways:

* `Animal_Builder.builder()` returns a builder filled with `null`, `0`, `false` and `Optional.empty()`.
* If you already have an instance of `Animal`, `Animal_Builder.toBuilder(Animal input)`
can be used to create a modified copy.

#### Aliasing the builder methods

It is often convenient to add these the alias methods `builder` and `toBuilder`
to `Animal`:

````java
@AutoBuilder
@AutoValue
abstract class Animal {
  abstract String name();
  abstract int numberOfLegs();

  static Animal_Builder builder() {
    return Animal_Builder.builder();
  }

  Animal_Builder toBuilder() {
    return Animal_Builder.toBuilder(this);
  }
}
````

#### Usage

Given the above example, we can create an
animal as follows:

````java
Animal betty = Animal.builder()
  .name("Betty")
  .numberOfLegs(4)
  .build();
````

Note that the runtime type of `betty` is `AutoValue_Animal`.
This processor is just a convenience to create "regular"
auto-value objects.

Updating the immutable object `betty` is straightforward:

````java
Animal sally = betty.toBuilder()
  .name("Sally")
  .build();
````

#### Internals

This annotation processor scans the generated class `AutoValue_Animal`,
rather than `Animal` itself.
Specifically, it looks at the constructor of `AutoValue_Animal`
to figure out what to do.

If `AutoValue_Animal` can't be found at the end of processing,
because the auto-value processing failed,
then auto-builder will not generate anything either.

#### Reducing the garbage collection overhead

Since version 1.5, auto-builder is capable of caching the builder instance.
This should in theory reduce the garbage collection overhead
(I have not measured it, though).

Use `reuseBuilder = true` to make
the generated code cache and re-use
the same builder instance:

````java
@AutoBuilder(reuseBuilder = true)
@AutoValue
abstract class Animal {
  // [...]
}
````

#### Configuration

The annotations are in a separate jar.
They are not needed at runtime, so the scope can be `optional`
or `provided`.

````xml
<dependency>
  <groupId>com.github.h908714124</groupId>
  <artifactId>auto-builder-annotations</artifactId>
  <version>1.0</version>
  <scope>provided</scope>
</dependency>
<dependency>
  <groupId>com.google.auto.value</groupId>
  <artifactId>auto-value-annotations</artifactId>
  <version>1.6.3</version>
</dependency>
````

The processor itself is only needed on the compiler classpath.

````xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <version>3.8.0</version>
      <configuration>
        <annotationProcessorPaths>
          <dependency>
            <groupId>com.github.h908714124</groupId>
            <artifactId>auto-builder</artifactId>
            <version>2.9.1</version>
          </dependency>
          <dependency>
            <groupId>com.google.auto.value</groupId>
            <artifactId>auto-value</artifactId>
            <version>1.6.3</version>
          </dependency>
        </annotationProcessorPaths>
      </configuration>
    </plugin>
  </plugins>
</build>
````

The equivalent gradle configuration looks as follows:
 
 ````groovy
dependencies {
   // Use 'api' rather than 'compile' for Android or java-library projects.
    compile "com.google.auto.value:auto-value-annotations:1.6.3"
    compileOnly "com.github.h908714124:auto-builder-annotations:1.0"
    annotationProcessor "com.google.auto.value:auto-value:1.6.3"
    annotationProcessor "com.github.h908714124:auto-builder:2.9.1"
}
 ````
  