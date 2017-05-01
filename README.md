# auto-builder

This very simple annotation processor relieves the 
user of the `@AutoValue.Builder` drudgery.
The generated class `*_Builder` should be an identical replacement.

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

* `AnimalBuilder.builder()` to create a builder filled with `null`, `0` and `false`.
* `AnimalBuilder.builder(Animal source)` creates a builder initialized from `source`, suitable for creating a modified copy.

The `static Animal create` method is not necessary for  `@AutoBuilder` to work.
In fact, this annotation processor scans the generated class `AutoValue_Animal`, rather than `Animal` itself.
If it can't find `AutoValue_Animal` on the class path,
presumably because auto-value is misconfigured or threw an error, it will not generate anything either.

### It's maven time

````java
<dependency>
  <groupId>com.github.h908714124</groupId>
  <artifactId>auto-builder</artifactId>
  <version>1.0</version>
  <scope>provided</scope>
</dependency>
````
