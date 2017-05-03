# auto-builder

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.h908714124/auto-builder/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.h908714124/auto-builder)

This very simple annotation processor relieves the 
user of the repetitive `@AutoValue.Builder` drudgery.
The generated `*_Builder.java` should be an equivalent replacement.

The single purpose of auto-builder is to make [auto-value](https://github.com/google/auto/tree/master/value)
more convenient.
If you're not using auto-value,
then auto-builder will not be helpful.

### Quick start

Add the `@AutoBuilder` annotation to a normal (non-builder) `@AutoValue` class.
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

A class `Animal_Builder` will be created in the same package.
The builder instance can be created in one of two ways:

* `Animal_Builder.builder()` to create a builder filled with `null`, `0` and `false`.
* `Animal_Builder.builder(Animal input)` creates a builder initialized from `input`, suitable for creating a modified copy.

The `static Animal create` method is not necessary for  `@AutoBuilder` to work.
In fact, this annotation processor scans the generated class `AutoValue_Animal`, rather than `Animal` itself.
If it can't find `AutoValue_Animal` on the classpath,
presumably because auto-value is misconfigured or threw an error, it won't generate anything either.

### It's maven time

````xml
<dependency>
  <groupId>com.github.h908714124</groupId>
  <artifactId>auto-builder</artifactId>
  <version>1.2</version>
  <scope>provided</scope>
</dependency>
````
