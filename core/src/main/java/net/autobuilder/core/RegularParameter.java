package net.autobuilder.core;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.autobuilder.core.Cleanse.preventNamingCollisions;
import static net.autobuilder.core.Util.downcase;
import static net.autobuilder.core.Util.upcase;

/**
 * Represents a &quot;normal&quot; parameter of the auto-value constructor.
 */
public final class RegularParameter extends Parameter {

  private static final Pattern GETTER_PATTERN =
      Pattern.compile("^get[A-Z].*$");
  private static final Pattern IS_PATTERN =
      Pattern.compile("^is[A-Z].*$");

  // A parameter of the auto-value constructor
  final VariableElement variableElement;

  public final String setterName;

  final String getterName;

  final TypeName generatedClass;

  private final boolean isPublic;

  private RegularParameter(
      VariableElement variableElement,
      String setterName,
      String getterName,
      TypeName generatedClass,
      boolean isPublic) {
    this.variableElement = variableElement;
    this.setterName = setterName;
    this.getterName = getterName;
    this.generatedClass = generatedClass;
    this.isPublic = isPublic;
  }

  static List<Parameter> scan(
      TypeName generatedClass,
      boolean isPublic,
      ExecutableElement avConstructor,
      TypeElement avType) {
    Set<String> methodNames = methodNames(avType);
    List<? extends VariableElement> rawParameters = avConstructor.getParameters();
    List<Parameter> avConstructorParameters = rawParameters.stream()
        .map(variableElement -> {
          String name = variableElement.getSimpleName().toString();
          TypeMirror type = variableElement.asType();
          String getterName = matchingAccessor(methodNames, variableElement);
          String setterName = setterName(name, type);
          RegularParameter parameter = new RegularParameter(
              variableElement, setterName, getterName, generatedClass, isPublic);
          return CollectionParameter.maybeCreate(parameter)
              .orElse(OptionalParameter.maybeCreate(parameter)
                  .orElse(parameter));
        })
        .collect(toList());
    return preventNamingCollisions(avConstructorParameters);
  }

  private static String setterName(String name, TypeMirror type) {
    if (GETTER_PATTERN.matcher(name).matches()) {
      return downcase(name.substring(3));
    }
    if (type.getKind() == TypeKind.BOOLEAN &&
        IS_PATTERN.matcher(name).matches()) {
      return downcase(name.substring(2));
    }
    return name;
  }

  private static Set<String> methodNames(TypeElement avType) {
    return ElementFilter.methodsIn(
        avType.getEnclosedElements()).stream()
        .filter(m -> m.getParameters().isEmpty())
        .filter(m -> m.getReturnType().getKind() != TypeKind.VOID)
        .map(ExecutableElement::getSimpleName)
        .map(CharSequence::toString)
        .collect(Collectors.toSet());
  }

  private static String matchingAccessor(Set<String> methodNames,
                                         VariableElement constructorArgument) {
    String name = constructorArgument.getSimpleName().toString();
    TypeName type = TypeName.get(constructorArgument.asType());
    if (methodNames.contains(name)) {
      return name;
    }
    if (type.equals(TypeName.BOOLEAN)) {
      String getter = "is" + upcase(name);
      if (methodNames.contains(getter)) {
        return getter;
      }
    }
    String getter = "get" + upcase(name);
    if (!methodNames.contains(getter)) {
      throw new ValidationException("no matching accessor: " + name, constructorArgument);
    }
    return getter;
  }

  /**
   * Gets the associated builder field for this parameter.
   */
  public FieldSpec asField() {
    return FieldSpec.builder(TypeName.get(type()), setterName).addModifiers(PRIVATE).build();
  }

  /**
   * Use original setter name, to prevent potential naming collision.
   */
  RegularParameter originalNames() {
    return new RegularParameter(variableElement, variableElement.getSimpleName().toString(),
        getterName, generatedClass, isPublic);
  }

  public TypeMirror type() {
    return variableElement.asType();
  }

  Modifier[] maybePublic() {
    if (isPublic) {
      return new Modifier[]{PUBLIC};
    }
    return new Modifier[]{};
  }


  @Override
  <R, P> R accept(ParamCases<R, P> cases, P p) {
    return cases.parameter(this, p);
  }
}
