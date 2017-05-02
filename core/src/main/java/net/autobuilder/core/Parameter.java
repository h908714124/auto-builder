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

  final String name;
  final String cleanName;
  final TypeName type;

  private Parameter(String name, String cleanName, TypeName type) {
    this.name = name;
    this.cleanName = cleanName;
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
      // auto-value changed its gen code?
      if (!methodNames.contains(name)) {
        throw new ValidationException("no matching getter: " + name, variableElement);
      }
      if (GETTER_PATTERN.matcher(name).matches()) {
        cleanName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
      } else if (type.equals(TypeName.BOOLEAN) &&
          IS_PATTERN.matcher(name).matches()) {
        cleanName = Character.toLowerCase(name.charAt(2)) + name.substring(3);
      }
      parameters.add(new Parameter(name, cleanName, type));
    }
    Set<String> duplicateCheck = parameters.stream()
        .map(p -> p.cleanName)
        .collect(toSet());
    if (duplicateCheck.size() < parameters.size()) {
      return parameters.stream()
          .map(p -> new Parameter(p.name, p.name, p.type))
          .collect(toList());
    }
    return parameters;
  }
}
