package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

final class Model {

  private static final String SUFFIX = "_Builder";

  final TypeName sourceClass;
  final TypeName generatedClass;
  final TypeElement avType;
  final List<Parameter> parameters;
  final Map<String, ExecutableElement> getters;
  final List<TypeVariableName> typevars;

  private Model(TypeName sourceClass,
                TypeName generatedClass, TypeElement avType, ExecutableElement avConstructor,
                Map<String, ExecutableElement> getters,
                List<TypeVariableName> typevars) {
    this.sourceClass = sourceClass;
    this.generatedClass = generatedClass;
    this.avType = avType;
    this.parameters = Parameter.scan(avConstructor, avType);
    this.getters = getters;
    this.typevars = typevars;
  }

  static Model create(TypeName sourceClass, TypeElement avType) {
    List<ExecutableElement> constructors = ElementFilter.constructorsIn(
        avType.getEnclosedElements());
    if (constructors.size() != 1) {
      throw new ValidationException(
          "Expecting exactly one constructor", avType);
    }
    Map<String, List<ExecutableElement>> methods = ElementFilter.methodsIn(
        avType.getEnclosedElements()).stream()
        .collect(Collectors.groupingBy(m ->
            m.getSimpleName().toString()));
    ExecutableElement constructor = constructors.get(0);
    Map<String, ExecutableElement> getters =
        new HashMap<>(constructor.getParameters().size());
    for (VariableElement parameter : constructor.getParameters()) {
      ExecutableElement getter = findGetter(parameter, methods);
      if (getter == null) {
        throw new ValidationException("Getter not found for '" +
            parameter.getSimpleName() + "'", avType);
      }
      getters.put(parameter.getSimpleName().toString(), getter);
    }
    List<TypeVariableName> typevars = avType.getTypeParameters().stream()
        .map(TypeVariableName::get)
        .collect(toList());
    return new Model(sourceClass, abPeer(sourceClass), avType,
        constructor, getters, typevars);
  }

  private static ExecutableElement findGetter(VariableElement parameter, Map<String, List<ExecutableElement>> methods) {
    String param = parameter.getSimpleName().toString();
    String upcaseParam = Character.toUpperCase(parameter.getSimpleName().charAt(0)) +
        param.substring(1);
    String[] names = TypeName.get(parameter.asType()).box().equals(TypeName.BOOLEAN) ?
        new String[]{"get" + upcaseParam, "is" + upcaseParam, param} :
        new String[]{"get" + upcaseParam, param};
    for (String name : names) {
      List<ExecutableElement> m = methods.get(name);
      if (m == null) {
        continue;
      }
      for (ExecutableElement method : m) {
        if (method.getParameters().isEmpty() &&
            TypeName.get(method.getReturnType()).equals(
                TypeName.get(parameter.asType()))) {
          return method;
        }
      }
    }
    return null;
  }

  private static TypeName abPeer(TypeName type) {
    String name = String.join("_", Processor.rawType(type).simpleNames()) + SUFFIX;
    ClassName className = Processor.rawType(type).topLevelClassName().peerClass(name);
    List<TypeName> typeargs = Processor.typeArguments(type);
    if (typeargs.isEmpty()) {
      return className;
    }
    return ParameterizedTypeName.get(className, typeargs.toArray(
        new TypeName[typeargs.size()]));
  }


}
