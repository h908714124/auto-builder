package net.autobuilder.examples;

import com.google.auto.value.AutoValue;
import net.autobuilder.AutoBuilder;
import net.autobuilder.PostBuild;

import java.io.IOException;

@AutoBuilder
@AutoValue
abstract class PostBuildAnimal {

  abstract String getSnake();

  @PostBuild
  PostBuildAnimal build() throws IOException {
    if (getSnake() == null || getSnake().isEmpty()) {
      throw new IOException("missing snake");
    }
    return this;
  }
}
