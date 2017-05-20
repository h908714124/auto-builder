# auto-builder

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.h908714124/auto-builder/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.h908714124/auto-builder)

This ~~very~~ simple annotation processor relieves the 
user of the repetitive drudgery that comes with `@AutoValue.Builder`.
The repetition of property names is, by auto-value's design choice 
not to generate any APIs, but only to extend user-defined classes, also present in
[some auto-value extensions](https://github.com/gabrielittner/auto-value-with).

The purpose of [auto-builder](https://github.com/h908714124/auto-builder) is to make 
[auto-value](https://github.com/google/auto/tree/master/value)
more convenient (and more efficient, see <a href="#caching">caching</a>).
If you're not using auto-value with builders, then auto-builder will not be helpful.
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
An instance of `Animal_Builder` can be obtained in one of two ways:

* `Animal_Builder.builder()` to create a builder filled with `null`, `0`, `false` and `Optional.empty()`.
* `Animal_Builder.builder(Animal input)` makes a builder initialized from `input`, suitable for creating a modified copy.

It might be a good idea to add the usual `toBuilder` convenience method:

````java
@AutoBuilder
@AutoValue
abstract class Animal {

  // [...]

  final Animal_Builder toBuilder() {
    return Animal_Builder.builder(this);
  }
}
````

The `static Animal create` method is not necessary for  `@AutoBuilder` to work.
In fact, this annotation processor scans the generated class `AutoValue_Animal`, rather than `Animal` itself.
If it can't find `AutoValue_Animal` on the classpath,
presumably because auto-value is misconfigured or threw an error, it won't generate anything useful either.

### Caching

Since version 1.5, auto-builder is capable of caching the builder instance.
This should in general help to reduce the garbage collection overhead.

Unless your `Animal` has type parameters (like `Animal<X>`),
a third static method `Animal_Builder.perThreadFactory()` is now generated, which returns a factory.

> This factory is safe for use by a single thread, but it <em>must not</em> be shared between different threads.
> If you're going to store the factory in a field,
> you have to wrap it in a `ThreadLocal`, as shown below.

#### Example: Wrapping the factory in a ThreadLocal

````java
@AutoBuilder
@AutoValue
abstract class Animal {

  private static final ThreadLocal<Animal_Builder.PerThreadFactory> FACTORY =
      ThreadLocal.withInitial(Animal_Builder::perThreadFactory);

  // [...]

  final Animal_Builder toBuilder() {
    return FACTORY.get().builder(this);
  }
}
````

Of course, the builder instance that's returned by `toBuilder` is also not thread-safe,
and shouldn't be stored in a field where other threads might see it.

### It's maven time

````xml
<dependency>
  <groupId>com.github.h908714124</groupId>
  <artifactId>auto-builder</artifactId>
  <version>2.1</version>
  <scope>provided</scope>
</dependency>
````
