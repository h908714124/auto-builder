package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.lang.Character.isLowerCase;
import static java.lang.Character.isUpperCase;
import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;
import static java.util.Collections.emptySet;
import static net.autobuilder.core.AutoBuilderProcessor.rawType;

final class Util {

  private static final CodeBlock emptyCodeBlock = CodeBlock.of("");

  private final ProcessingEnvironment processingEnv;

  Util(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
  }

  TypeName subtypeOf(TypeName typeName) {
    if (typeName.isPrimitive()) {
      return typeName;
    }
    if (typeName instanceof WildcardTypeName) {
      return typeName;
    }
    if (!(typeName instanceof ClassName || typeName instanceof ParameterizedTypeName)) {
      return WildcardTypeName.subtypeOf(typeName);
    }
    ClassName className = rawType(typeName);
    TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(
        className.packageName() + '.' + String.join(".", className.simpleNames()));
    if (typeElement == null) {
      return WildcardTypeName.subtypeOf(typeName);
    }
    if (typeElement.getModifiers().contains(Modifier.FINAL)) {
      return typeName;
    }
    return WildcardTypeName.subtypeOf(typeName);
  }

  static String upcase(String s) {
    if (s.isEmpty() || isUpperCase(s.charAt(0))) {
      return s;
    }
    return toUpperCase(s.charAt(0)) + s.substring(1);
  }

  static String downcase(String s) {
    if (s.isEmpty() || isLowerCase(s.charAt(0))) {
      return s;
    }
    return toLowerCase(s.charAt(0)) + s.substring(1);
  }

  static <E> Collector<E, List<E>, Boolean> isDistinct() {
    return new Collector<E, List<E>, Boolean>() {
      @Override
      public Supplier<List<E>> supplier() {
        return ArrayList::new;
      }

      @Override
      public BiConsumer<List<E>, E> accumulator() {
        return List::add;
      }

      @Override
      public BinaryOperator<List<E>> combiner() {
        return (left, right) -> {
          left.addAll(right);
          return left;
        };
      }

      @Override
      public Function<List<E>, Boolean> finisher() {
        return elements -> {
          Set<E> set = new HashSet<>();
          for (E element : elements) {
            if (!set.add(element)) {
              return false;
            }
          }
          return true;
        };
      }

      @Override
      public Set<Characteristics> characteristics() {
        return emptySet();
      }
    };
  }
  static Collector<CodeBlock, List<CodeBlock>, CodeBlock> joinCodeBlocks(String delimiter) {
    return new Collector<CodeBlock, List<CodeBlock>, CodeBlock>() {
      @Override
      public Supplier<List<CodeBlock>> supplier() {
        return ArrayList::new;
      }

      @Override
      public BiConsumer<List<CodeBlock>, CodeBlock> accumulator() {
        return List::add;
      }

      @Override
      public BinaryOperator<List<CodeBlock>> combiner() {
        return (left, right) -> {
          left.addAll(right);
          return left;
        };
      }

      @Override
      public Function<List<CodeBlock>, CodeBlock> finisher() {
        return blocks -> {
          if (blocks.isEmpty()) {
            return emptyCodeBlock;
          }
          CodeBlock.Builder builder = CodeBlock.builder();
          for (int i = 0; i < blocks.size() - 1; i++) {
            builder.add(blocks.get(i));
            if (!delimiter.isEmpty()) {
              builder.add(delimiter);
            }
          }
          builder.add(blocks.get(blocks.size() - 1));
          return builder.build();
        };
      }

      @Override
      public Set<Characteristics> characteristics() {
        return emptySet();
      }
    };
  }
}
