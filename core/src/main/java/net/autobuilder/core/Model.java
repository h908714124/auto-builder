package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import net.autobuilder.AutoBuilder;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.Arrays;
import java.util.List;

import static javax.lang.model.element.Modifier.PUBLIC;
import static net.autobuilder.core.AutoBuilderProcessor.rawType;

public final class Model {

  private static final String SUFFIX = "_Builder";

  private final TypeElement sourceElement;

  // The type that auto-value has generated
  final TypeElement avElement;

  // should gen code reuse builder instances?
  final boolean reuse;

  public final List<ParaParameter> parameters;

  final TypeName generatedClass;

  private Model(
      TypeElement sourceElement,
      TypeName generatedClass,
      TypeElement avElement,
      boolean reuse,
      List<ParaParameter> parameters) {
    this.reuse = reuse;
    this.generatedClass = generatedClass;
    this.sourceElement = sourceElement;
    this.avElement = avElement;
    this.parameters = parameters;
  }

  static Model create(
      List<ParaParameter> parameters,
      TypeElement sourceElement,
      TypeElement avElement) {
    ExecutableElement avConstructor = getAvConstructor(sourceElement, avElement);
    if (avConstructor.getModifiers().contains(Modifier.PRIVATE)) {
      boolean suspicious = ElementFilter.typesIn(sourceElement.getEnclosedElements())
          .stream()
          .anyMatch(
              e -> e.getAnnotationMirrors().stream().anyMatch(annotationMirror -> {
                ClassName className = rawType(TypeName.get(annotationMirror.getAnnotationType()));
                return className.packageName().equals("com.google.auto.value") &&
                    className.simpleNames().equals(Arrays.asList("AutoValue", "Builder"));
              }));
      if (suspicious) {
        throw new ValidationException(
            sourceElement + ": @AutoBuilder and @AutoValue.Builder cannot be used together.",
            sourceElement);
      }
      throw new ValidationException(
          "Expecting the generated auto-value class to have a non-private constructor.",
          sourceElement);
    }
    TypeName generatedClass = generatedClass(sourceElement);
    if (!sourceElement.getTypeParameters().isEmpty()) {
      throw new ValidationException("The class may not have type parameters.",
          sourceElement);
    }
    boolean optionalRefTrackingBuilderClass =
        sourceElement.getAnnotation(AutoBuilder.class).reuseBuilder();
    return new Model(sourceElement, generatedClass, avElement,
        optionalRefTrackingBuilderClass, parameters);
  }

  static ExecutableElement getAvConstructor(TypeElement sourceElement, TypeElement avElement) {
    List<ExecutableElement> avConstructors = ElementFilter.constructorsIn(
        avElement.getEnclosedElements());
    if (avConstructors.size() != 1) {
      throw new ValidationException(
          "Expecting the generated auto-value class to have exactly one constructor.",
          sourceElement);
    }
    return avConstructors.get(0);
  }

  static TypeName generatedClass(TypeElement typeElement) {
    String name = String.join("_", ClassName.get(typeElement).simpleNames()) + SUFFIX;
    return ClassName.get(typeElement).topLevelClassName().peerClass(name);
  }

  static TypeName withTypevars(ClassName className, TypeName[] typevars) {
    if (typevars.length == 0) {
      return className;
    }
    return ParameterizedTypeName.get(className, typevars);
  }

  private boolean isPublic() {
    return sourceElement.getModifiers().contains(PUBLIC);
  }

  Modifier[] maybePublic() {
    if (isPublic()) {
      return new Modifier[]{PUBLIC};
    }
    return new Modifier[]{};
  }

  TypeElement sourceElement() {
    return sourceElement;
  }

  ClassName perThreadFactoryClass() {
    return rawType(generatedClass)
        .nestedClass("PerThreadFactory");
  }

  String uniqueFieldName(String baseName) {
    while (isFieldNameCollision(baseName)) {
      baseName = "_" + baseName;
    }
    return baseName;
  }

  String uniqueSetterMethodName(String baseName) {
    while (isSetterMethodNameCollision(baseName)) {
      baseName = "_" + baseName;
    }
    return baseName;
  }

  private boolean isFieldNameCollision(
      String fieldName) {
    for (ParaParameter parameter : parameters) {
      if (parameter.getParameter().asField().name.equals(fieldName)) {
        return true;
      }
    }
    return false;
  }

  boolean isSetterMethodNameCollision(String methodName) {
    for (ParaParameter parameter : parameters) {
      if (parameter.getParameter().setterName.equals(methodName)) {
        return true;
      }
    }
    return false;
  }

}
