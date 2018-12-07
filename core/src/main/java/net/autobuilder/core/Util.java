package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.lang.model.util.SimpleTypeVisitor8;
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

final class Util {

  static final SimpleTypeVisitor8<DeclaredType, Void> AS_DECLARED =
      new SimpleTypeVisitor8<DeclaredType, Void>() {
        @Override
        public DeclaredType visitDeclared(DeclaredType declaredType, Void _null) {
          return declaredType;
        }
      };

  static final SimpleElementVisitor8<TypeElement, Void> AS_TYPE_ELEMENT =
      new SimpleElementVisitor8<TypeElement, Void>() {
        @Override
        public TypeElement visitType(TypeElement typeElement, Void _null) {
          return typeElement;
        }
      };

  static boolean equalsType(TypeMirror typeMirror, String qualified) {
    DeclaredType declared = typeMirror.accept(AS_DECLARED, null);
    if (declared == null) {
      return false;
    }
    TypeElement typeElement = declared.asElement().accept(AS_TYPE_ELEMENT, null);
    if (typeElement == null) {
      return false;
    }
    return typeElement.getQualifiedName().toString().equals(qualified);
  }

  private final ProcessingEnvironment processingEnv;

  Util(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
  }

  static TypeName[] typeArgumentSubtypes(VariableElement variableElement) {
    DeclaredType declaredType = asDeclared(variableElement);
    TypeTool tool = TypeTool.get();
    return declaredType.getTypeArguments().stream()
        .map(tool::asExtendsWildcard)
        .map(TypeName::get)
        .toArray(TypeName[]::new);
  }

  static TypeName[] typeArguments(VariableElement variableElement) {
    DeclaredType declaredType = asDeclared(variableElement);
    return declaredType.getTypeArguments().stream()
        .map(TypeName::get)
        .toArray(TypeName[]::new);
  }

  private static DeclaredType asDeclared(VariableElement variableElement) {
    DeclaredType declaredType = variableElement.asType().accept(AS_DECLARED, null);
    if (declaredType == null) {
      throw new AssertionError();
    }
    return declaredType;
  }

  static TypeName[] typeArguments(TypeMirror typeMirror) {
    DeclaredType type = typeMirror.accept(AS_DECLARED, null);
    return type.getTypeArguments().stream()
        .map(TypeName::get)
        .toArray(TypeName[]::new);
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
          CodeBlock.Builder builder = CodeBlock.builder();
          if (blocks.isEmpty()) {
            return builder.build();
          }
          builder.add(blocks.get(0));
          blocks.stream().skip(1).forEach(block ->
              builder.add(delimiter).add(block));
          return builder.build();
        };
      }

      @Override
      public Set<Characteristics> characteristics() {
        return emptySet();
      }
    };
  }

  TypeElement typeElement(ClassName className) {
    return processingEnv.getElementUtils().getTypeElement(
        className.toString());
  }

  static ClassName className(String qualifiedName) {
    return ClassName.get(TypeTool.get().getTypeElement(qualifiedName));
  }
}
