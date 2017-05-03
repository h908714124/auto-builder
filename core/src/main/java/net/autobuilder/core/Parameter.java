package net.autobuilder.core;

import com.squareup.javapoet.TypeName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

final class Parameter {

  private static final Pattern GETTER_PATTERN =
      Pattern.compile("^get[A-Z].*$");
  private static final Pattern IS_PATTERN =
      Pattern.compile("^is[A-Z].*$");

  private final String originalName;
  final String cleanName;
  final String methodName;
  final TypeName type;

  private Parameter(String originalName,
                    String cleanName,
                    String methodName,
                    TypeName type) {
    this.originalName = originalName;
    this.cleanName = cleanName;
    this.methodName = methodName;
    this.type = type;
  }

  static List<Parameter> scan(ExecutableElement constructor,
                              TypeElement avType) {
    Set<String> methodNames = ElementFilter.methodsIn(
        avType.getEnclosedElements()).stream()
        .filter(m -> m.getParameters().isEmpty())
        .filter(m -> !m.getReturnType().equals(TypeName.VOID))
        .map(ExecutableElement::getSimpleName)
        .map(CharSequence::toString)
        .collect(Collectors.toSet());
    List<Parameter> parameters = new ArrayList<>();
    for (VariableElement variableElement : constructor.getParameters()) {
      String name = variableElement.getSimpleName().toString();
      String cleanName = name;
      TypeName type = TypeName.get(variableElement.asType());
      String methodName = methodName(methodNames, variableElement);
      if (GETTER_PATTERN.matcher(name).matches()) {
        cleanName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
      } else if (type.equals(TypeName.BOOLEAN) &&
          IS_PATTERN.matcher(name).matches()) {
        cleanName = Character.toLowerCase(name.charAt(2)) + name.substring(3);
      }
      parameters.add(new Parameter(name, cleanName, methodName, type));
    }
    Set<String> duplicateCheck = parameters.stream()
        .map(p -> p.cleanName)
        .collect(toSet());
    if (duplicateCheck.size() < parameters.size()) {
      return parameters.stream()
          .map(p -> new Parameter(p.originalName, p.originalName, p.methodName, p.type))
          .collect(toList());
    }
    return parameters;
  }

  private static String methodName(Set<String> methodNames,
                                   VariableElement variableElement) {
    String name = variableElement.getSimpleName().toString();
    TypeName type = TypeName.get(variableElement.asType());
    if (methodNames.contains(name)) {
      return name;
    }
    if (type.equals(TypeName.BOOLEAN)) {
      String getter = "is" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
      if (methodNames.contains(getter)) {
        return getter;
      }
    }
    String getter = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    if (!methodNames.contains(getter)) {
      throw new ValidationException("no matching getter: " + name, variableElement);
    }
    return getter;
  }
}
