package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.autobuilder.core.Collectionish.CollectionType;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.autobuilder.core.AutoBuilderProcessor.rawType;
import static net.autobuilder.core.ParaParameter.asConsumer;
import static net.autobuilder.core.ParaParameter.asFunction;
import static net.autobuilder.core.ParaParameter.biFunction;
import static net.autobuilder.core.Util.typeArgumentSubtypes;

final class Model {

  private static final String SUFFIX = "_Builder";
  private static final Modifier[] PUBLIC_MODIFIER = {PUBLIC};
  private static final Modifier[] NO_MODIFIERS = {};
  private static final String REF_TRACKING_BUILDER = "RefTrackingBuilder";
  private static final String SIMPLE_BUILDER = "SimpleBuilder";

  private final TypeElement sourceClassElement;

  private final ClassName optionalRefTrackingBuilderClass;

  private final ExecutableElement constructor;

  final TypeName generatedClass;
  final TypeName simpleBuilderClass;
  final TypeElement avType;
  final TypeName sourceClass;
  final Util util;

  private Model(Util util, TypeElement sourceClassElement,
                TypeName generatedClass,
                TypeElement avType,
                TypeName simpleBuilderClass,
                ClassName optionalRefTrackingBuilderClass,
                ExecutableElement constructor) {
    this.util = util;
    this.sourceClassElement = sourceClassElement;
    this.generatedClass = generatedClass;
    this.avType = avType;
    this.simpleBuilderClass = simpleBuilderClass;
    this.optionalRefTrackingBuilderClass = optionalRefTrackingBuilderClass;
    this.sourceClass = TypeName.get(sourceClassElement.asType());
    this.constructor = constructor;
  }

  static Model create(
      Util util,
      TypeElement sourceClassElement, TypeElement avType) {
    TypeName sourceClass = TypeName.get(sourceClassElement.asType());
    List<ExecutableElement> constructors = ElementFilter.constructorsIn(
        avType.getEnclosedElements());
    if (constructors.size() != 1) {
      throw new ValidationException(
          avType + " does not have exactly one constructor.", sourceClassElement);
    }
    ExecutableElement constructor = constructors.get(0);
    if (constructor.getModifiers().contains(Modifier.PRIVATE)) {
      boolean suspicious = ElementFilter.typesIn(sourceClassElement.getEnclosedElements())
          .stream()
          .anyMatch(
              e -> e.getAnnotationMirrors().stream().anyMatch(annotationMirror -> {
                ClassName className = rawType(TypeName.get(annotationMirror.getAnnotationType()));
                return className.packageName().equals("com.google.auto.value") &&
                    className.simpleNames().equals(Arrays.asList("AutoValue", "Builder"));
              }));
      if (suspicious) {
        throw new ValidationException(
            sourceClassElement + ": @AutoBuilder and @AutoValue.Builder cannot be used together.",
            sourceClassElement);
      }
      throw new ValidationException(
          avType + " has a private constructor.",
          sourceClassElement);
    }
    TypeName generatedClass = generatedClass(sourceClass);
    TypeName simpleBuilderClass = simpleBuilderClass(generatedClass);
    ClassName optionalRefTrackingBuilderClass =
        typeArguments(generatedClass).isEmpty() ?
            rawType(generatedClass).nestedClass(REF_TRACKING_BUILDER) :
            null;
    return new Model(util, sourceClassElement, generatedClass, avType,
        simpleBuilderClass,
        optionalRefTrackingBuilderClass, constructor);
  }

  List<ParaParameter> scan() {
    return Parameter.scan(this, util, constructor, avType);
  }

  private static TypeName generatedClass(TypeName type) {
    String name = String.join("_", rawType(type).simpleNames()) + SUFFIX;
    ClassName className = rawType(type).topLevelClassName().peerClass(name);
    return withTypevars(className, typeArguments(type));
  }

  private static TypeName simpleBuilderClass(TypeName generatedClass) {
    return withTypevars(rawType(generatedClass).nestedClass(SIMPLE_BUILDER),
        typeArguments(generatedClass));
  }

  private static TypeName withTypevars(ClassName className, List<TypeName> typevars) {
    if (typevars.isEmpty()) {
      return className;
    }
    return ParameterizedTypeName.get(className, typevars.toArray(
        new TypeName[typevars.size()]));
  }

  List<TypeVariableName> typevars() {
    return avType.getTypeParameters().stream()
        .map(TypeVariableName::get)
        .collect(toList());
  }

  private boolean isPublic() {
    return sourceClassElement.getModifiers().contains(PUBLIC);
  }

  Modifier[] maybePublic() {
    if (isPublic()) {
      return PUBLIC_MODIFIER;
    }
    return NO_MODIFIERS;
  }

  Optional<ClassName> optionalRefTrackingBuilderClass() {
    return Optional.ofNullable(optionalRefTrackingBuilderClass);
  }

  String cacheWarning() {
    return "Caching not implemented: " +
        rawType(sourceClass).simpleName() +
        "<" +
        typevars().stream()
            .map(TypeVariableName::toString)
            .collect(joining(", ")) +
        "> has type parameters";
  }

  private static List<TypeName> typeArguments(TypeName typeName) {
    if (typeName instanceof ParameterizedTypeName) {
      return ((ParameterizedTypeName) typeName).typeArguments;
    }
    return Collections.emptyList();
  }

  Function<ParaParameter, Parameter> getParameter =
      asFunction(new ParaParameter.Cases<Parameter, Void>() {
        @Override
        Parameter parameter(Parameter parameter, Void _null) {
          return parameter;
        }

        @Override
        Parameter collectionish(Collectionish collectionish, Void _null) {
          return collectionish.parameter;
        }

        @Override
        Parameter optionalish(Optionalish optionalish, Void _null) {
          return optionalish.parameter;
        }
      });

  Function<ParaParameter, List<String>> methodNames =
      asFunction(new ParaParameter.Cases<List<String>, Void>() {
        @Override
        List<String> parameter(Parameter parameter, Void _null) {
          return singletonList(parameter.setterName);
        }

        @Override
        List<String> collectionish(Collectionish collectionish, Void _null) {
          return collectionish.hasAccumulator() ?
              asList(collectionish.parameter.setterName, collectionish.accumulatorName()) :
              singletonList(collectionish.parameter.setterName);
        }

        @Override
        List<String> optionalish(Optionalish optionalish, Void _null) {
          return singletonList(optionalish.parameter.setterName);
        }
      });

  Function<ParaParameter, List<String>> fieldNames =
      asFunction(new ParaParameter.Cases<List<String>, Void>() {
        @Override
        List<String> parameter(Parameter parameter, Void _null) {
          return singletonList(parameter.setterName);
        }

        @Override
        List<String> collectionish(Collectionish collectionish, Void _null) {
          return collectionish.hasAccumulator() ?
              asList(collectionish.parameter.setterName, collectionish.builderFieldName()) :
              singletonList(collectionish.parameter.setterName);
        }

        @Override
        List<String> optionalish(Optionalish optionalish, Void _null) {
          return singletonList(optionalish.parameter.setterName);
        }
      });

  Function<ParaParameter, ParaParameter> noAccumulator =
      asFunction(new ParaParameter.Cases<ParaParameter, Void>() {
        @Override
        ParaParameter parameter(Parameter parameter, Void _null) {
          return parameter;
        }

        @Override
        ParaParameter collectionish(Collectionish collectionish, Void _null) {
          return collectionish.noAccumulator();
        }

        @Override
        ParaParameter optionalish(Optionalish optionalish, Void _null) {
          return optionalish;
        }
      });

  Function<ParaParameter, ParaParameter> originalSetter =
      asFunction(new ParaParameter.Cases<ParaParameter, Void>() {
        @Override
        ParaParameter parameter(Parameter parameter, Void _null) {
          return parameter.originalSetter();
        }

        @Override
        ParaParameter collectionish(Collectionish collectionish, Void _null) {
          return collectionish.withParameter(collectionish.parameter.originalSetter());
        }

        @Override
        ParaParameter optionalish(Optionalish optionalish, Void _null) {
          return optionalish.withParameter(optionalish.parameter.originalSetter());
        }
      });

  Function<ParaParameter, ParameterSpec> asParameter =
      asFunction(new ParaParameter.Cases<ParameterSpec, Void>() {
        @Override
        ParameterSpec parameter(Parameter parameter, Void _null) {
          return ParameterSpec.builder(parameter.type, parameter.setterName).build();
        }

        @Override
        ParameterSpec collectionish(Collectionish collectionish, Void _null) {
          TypeName type = collectionish.wildTyping ?
              ParameterizedTypeName.get(collectionish.setterParameterClassName,
                  typeArgumentSubtypes(
                      collectionish.parameter.variableElement)) :
              collectionish.parameter.type;
          return ParameterSpec.builder(type, collectionish.parameter.setterName).build();
        }

        @Override
        ParameterSpec optionalish(Optionalish optionalish, Void _null) {
          return ParameterSpec.builder(optionalish.parameter.type,
              optionalish.parameter.setterName).build();
        }
      });

  Function<ParaParameter, CodeBlock> setterAssignment =
      asFunction(new ParaParameter.Cases<CodeBlock, Void>() {
        @Override
        CodeBlock parameter(Parameter parameter, Void _null) {
          FieldSpec field = parameter.asField();
          ParameterSpec p = asParameter.apply(parameter);
          return CodeBlock.builder()
              .addStatement("this.$N = $N", field, p).build();
        }

        @Override
        CodeBlock collectionish(Collectionish collectionish, Void _null) {
          return collectionish.setterAssignment.apply(collectionish);
        }

        @Override
        CodeBlock optionalish(Optionalish optionalish, Void _null) {
          FieldSpec field = optionalish.parameter.asField();
          ParameterSpec p = asParameter.apply(optionalish);
          return CodeBlock.builder()
              .addStatement("this.$N = $N", field, p).build();
        }
      });

  BiFunction<ParaParameter, ParameterSpec, CodeBlock> getFieldValue =
      biFunction(new ParaParameter.Cases<CodeBlock, ParameterSpec>() {
        @Override
        CodeBlock parameter(Parameter parameter, ParameterSpec builder) {
          return CodeBlock.of("$N.$N", builder, parameter.asField());
        }

        @Override
        CodeBlock collectionish(Collectionish collectionish, ParameterSpec builder) {
          FieldSpec field = collectionish.parameter.asField();
          CodeBlock getCollection = CodeBlock.builder()
              .add("$N.$N != null ? $N.$N : ",
                  builder, field, builder, field)
              .add(collectionish.emptyBlock.get())
              .build();
          if (!collectionish.hasAccumulator()) {
            return getCollection;
          }
          FieldSpec builderField = collectionish.asBuilderField();
          return CodeBlock.builder()
              .add("$N.$N != null ? ", builder, builderField)
              .add(collectionish.buildBlock.apply(builder, builderField))
              .add(" :\n        ")
              .add(getCollection)
              .build();
        }

        @Override
        CodeBlock optionalish(Optionalish optionalish, ParameterSpec builder) {
          FieldSpec field = optionalish.parameter.asField();
          return CodeBlock.of("$N.$N != null ? $N.$N : $T.empty()",
              builder, field,
              builder, field,
              optionalish.wrapper);
        }
      });

  BiConsumer<ParaParameter, CodeBlock.Builder> clearAccumulator =
      asConsumer(new ParaParameter.Cases<Void, CodeBlock.Builder>() {
        @Override
        Void parameter(Parameter parameter, CodeBlock.Builder builder) {
          return null;
        }

        @Override
        Void collectionish(Collectionish collectionish, CodeBlock.Builder builder) {
          if (collectionish.hasAccumulator()) {
            builder.addStatement("this.$N = null",
                collectionish.asBuilderField());
          }
          return null;
        }

        @Override
        Void optionalish(Optionalish optionalish, CodeBlock.Builder builder) {
          return null;
        }
      });

  BiConsumer<ParaParameter, TypeSpec.Builder> addOptionalishOverload =
      asConsumer(new ParaParameter.Cases<Void, TypeSpec.Builder>() {
        @Override
        Void parameter(Parameter parameter, TypeSpec.Builder builder) {
          return null;
        }

        @Override
        Void collectionish(Collectionish collectionish, TypeSpec.Builder block) {
          return null;
        }

        @Override
        Void optionalish(Optionalish optionalish, TypeSpec.Builder builder) {
          if (!optionalish.convenienceOverload()) {
            return null;
          }
          FieldSpec f = optionalish.parameter.asField();
          ParameterSpec p = ParameterSpec.builder(optionalish.wrapped,
              optionalish.parameter.setterName).build();
          CodeBlock.Builder block = CodeBlock.builder();
          if (optionalish.isOptional()) {
            block.addStatement("this.$N = $T.$L($N)", f, optionalish.wrapper, optionalish.of, p);
          } else {
            block.addStatement("this.$N = $T.of($N)", f, optionalish.wrapper, p);
          }
          builder.addMethod(MethodSpec.methodBuilder(
              optionalish.parameter.setterName)
              .addCode(block.build())
              .addStatement("return this")
              .addParameter(p)
              .addModifiers(FINAL)
              .addModifiers(maybePublic())
              .returns(generatedClass)
              .build());
          return null;
        }
      });

  BiConsumer<ParaParameter, TypeSpec.Builder> addAccumulatorField =
      asConsumer(new ParaParameter.Cases<Void, TypeSpec.Builder>() {
        @Override
        Void parameter(Parameter parameter, TypeSpec.Builder builder) {
          return null;
        }

        @Override
        Void collectionish(Collectionish collectionish, TypeSpec.Builder builder) {
          if (!collectionish.hasAccumulator()) {
            return null;
          }
          builder.addField(collectionish.asBuilderField());
          return null;
        }

        @Override
        Void optionalish(Optionalish optionalish, TypeSpec.Builder builder) {
          return null;
        }
      });

  BiConsumer<ParaParameter, TypeSpec.Builder> addAccumulatorMethod =
      asConsumer(new ParaParameter.Cases<Void, TypeSpec.Builder>() {
        @Override
        Void parameter(Parameter parameter, TypeSpec.Builder builder) {
          return null;
        }

        @Override
        Void collectionish(Collectionish collectionish, TypeSpec.Builder builder) {
          if (!collectionish.hasAccumulator()) {
            return null;
          }
          builder.addMethod(collectionish.type == CollectionType.MAP ?
              collectionish.putInMethod(Model.this) :
              collectionish.addToMethod(Model.this));
          return null;
        }

        @Override
        Void optionalish(Optionalish optionalish, TypeSpec.Builder builder) {
          return null;
        }
      });

  BiConsumer<ParaParameter, TypeSpec.Builder> addAccumulatorOverload =
      asConsumer(new ParaParameter.Cases<Void, TypeSpec.Builder>() {
        @Override
        Void parameter(Parameter parameter, TypeSpec.Builder builder) {
          return null;
        }

        @Override
        Void collectionish(Collectionish collectionish, TypeSpec.Builder builder) {
          if (!collectionish.hasAccumulator()) {
            return null;
          }
          collectionish.addAllType.apply(collectionish).ifPresent(addAllType ->
              builder.addMethod(collectionish.type == CollectionType.MAP ?
                  collectionish.putAllInMethod(Model.this, addAllType) :
                  collectionish.addAllToMethod(Model.this, addAllType)));
          return null;
        }

        @Override
        Void optionalish(Optionalish optionalish, TypeSpec.Builder builder) {
          return null;
        }
      });

  BiFunction<ParaParameter, ParameterSpec, Optional<CodeBlock>> cleanupCode =
      biFunction(new ParaParameter.Cases<Optional<CodeBlock>, ParameterSpec>() {
        @Override
        Optional<CodeBlock> parameter(Parameter parameter, ParameterSpec builder) {
          if (parameter.type instanceof ClassName ||
              parameter.type instanceof ParameterizedTypeName) {
            return Optional.of(CodeBlock.builder().addStatement("$N.$L(null)",
                builder, parameter.setterName).build());
          }
          return Optional.empty();
        }
        @Override
        Optional<CodeBlock> collectionish(Collectionish collectionish, ParameterSpec builder) {
          return this.parameter(collectionish.parameter, builder);
        }
        @Override
        Optional<CodeBlock> optionalish(Optionalish optionalish, ParameterSpec builder) {
          if (optionalish.convenienceOverload()) {
            return Optional.of(CodeBlock.builder().addStatement("$N.$L(($T) null)",
                builder, optionalish.parameter.setterName,
                optionalish.parameter.type).build());
          }
          return this.parameter(optionalish.parameter, builder);
        }
      });
}
