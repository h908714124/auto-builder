## auto-builder

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.h908714124/auto-builder/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.h908714124/auto-builder)

Adding builder functionality to an [auto-value](https://github.com/google/auto/tree/master/value)
class normally requires copying each property's name and type.
For every property, you have to add a corresponding setter like this:

````java
@AutoValue
abstract class Animal {
  abstract String name(); // property "name" defined here
  // [...] 
  @AutoValue.Builder
  abstract class Builder {
    Builder name(String name); // ... and here!
    // [...]
  }
}

````

So far, auto-value extensions like [auto-value-with](https://github.com/gabrielittner/auto-value-with)
could not remove this redundancy.
I believe that it can't be done because auto-value, and even its extensions,
may only extend user-defined classes.
Therefore, the user has to maintain a redundant blueprint for the builder.

The purpose of [auto-builder](https://github.com/h908714124/auto-builder) is to make 
auto-value more convenient, by getting rid of the builder blueprint.

If you're not using auto-value, then auto-builder will not be helpful.
Depending on your use case, you may want to have a look at
[readable](https://github.com/h908714124/readable) instead.

### Quick start

1. Add the `@AutoBuilder` annotation to a normal (non-builder) `@AutoValue` class. 
1. Done! A class `*_Builder.java` will be generated in the same package.

The following is roughly equivalent to the `Animal` example from the
[auto-value docs](https://github.com/google/auto/blob/master/value/userguide/builders.md):

````java
@AutoBuilder
@AutoValue
abstract class Animal {
  abstract String name();
  abstract int numberOfLegs();

  static Animal create(String name, int numberOfLegs) {
    return new AutoValue_Animal(name, numberOfLegs);
  }
}
````

A class `Animal_Builder` will now be generated in the same package as `Animal`.
An instance of `Animal_Builder` can be obtained in two different ways:

* `Animal_Builder.builder()` returns a builder filled with `null`, `0`, `false` and `Optional.empty()`.
* `Animal_Builder.toBuilder(Animal input)` returns a builder initialized from `input`, suitable for creating a modified copy.

It can be convenient to add aliases of these methods to `Animal` itself:

````java
@AutoBuilder
@AutoValue
abstract class Animal {

  // [...]

  static Animal_Builder builder() {
    return Animal_Builder.builder();
  }

  final Animal_Builder toBuilder() {
    return Animal_Builder.toBuilder(this);
  }
}
````

### Configuration

This annotation processor scans the generated class `AutoValue_Animal`, rather than `Animal` itself.
Specifically, it looks at the constructor of that class to figure out what to do.

If `AutoValue_Animal` can't be found,
presumably because auto-value is misconfigured or threw an error,
then auto-builder will not generate anything either.

### Caching

Since version 1.5, auto-builder is capable of caching the builder instance.
This can often reduce the garbage collection overhead.

Use `reuseBuilder = true` to make
the generated code cache and re-use builder instances:

````java
@AutoBuilder(reuseBuilder = true)
@AutoValue
abstract class Animal {
  // [...]
}
````

### Maven info

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
  <version>${auto-value.version}</version>
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
            <version>${auto-builder.version}</version>
          </dependency>
          <dependency>
            <groupId>com.google.auto.value</groupId>
            <artifactId>auto-value</artifactId>
            <version>${auto-value.version}</version>
          </dependency>
        </annotationProcessorPaths>
      </configuration>
    </plugin>
  </plugins>
</build>
````
