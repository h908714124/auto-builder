package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.autobuilder.AutoBuilder;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static javax.lang.model.util.ElementFilter.typesIn;
import static javax.tools.Diagnostic.Kind.ERROR;

public final class Processor extends AbstractProcessor {

  private static final String SUFFIX = "_Builder";
  private static final String PREFIX = "AutoValue_";

  private final Set<TypeName> done = new HashSet<>();
  private final Set<TypeElement> seen = new HashSet<>();

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    HashSet<String> strings = new HashSet<>();
    strings.add(AutoBuilder.class.getCanonicalName());
    return strings;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
    Set<TypeElement> typeElements =
        typesIn(env.getElementsAnnotatedWith(AutoBuilder.class));
    for (TypeElement typeElement : seen) {
      TypeName sourceClass = maybeParameterized(typeElement);
      TypeElement avType = processingEnv.getElementUtils().getTypeElement(
          avPeer(sourceClass).toString());
      if (avType == null) {
        // auto-value isn't finished yet, skip this round
        continue;
      }
      processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "exist");
      Model model = Model.create(sourceClass, avType);
      if (!done.add(model.sourceClass)) {
        continue;
      }
      try {
        TypeSpec typeSpec = Analyser.create(model).analyse();
        write(rawType(model.generatedClass), typeSpec);
      } catch (ValidationException e) {
        processingEnv.getMessager().printMessage(ERROR, e.getMessage(), e.about);
      } catch (Exception e) {
        handleException(typeElement, e);
        return false;
      }
    }
    seen.addAll(typeElements);
    return false;
  }

  private void handleException(TypeElement typeElement, Exception e) {
    e.printStackTrace();
    String message = "Error processing " +
        ClassName.get(typeElement) +
        ": " + e.getMessage();
    processingEnv.getMessager().printMessage(ERROR, message, typeElement);
  }

  private void write(ClassName generatedType, TypeSpec typeSpec) throws IOException {
    JavaFile javaFile = JavaFile.builder(generatedType.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
    JavaFileObject sourceFile = processingEnv.getFiler()
        .createSourceFile(generatedType.toString(),
            javaFile.typeSpec.originatingElements.toArray(new Element[0]));
    try (Writer writer = sourceFile.openWriter()) {
      writer.write(javaFile.toString());
    }
  }

  static final class Model {
    final TypeName sourceClass;
    final TypeName generatedClass;
    final TypeElement avType;
    final ExecutableElement avConstructor;
    final Map<String, ExecutableElement> getters;
    final List<TypeVariableName> typevars;

    Model(TypeName sourceClass,
          TypeName generatedClass, TypeElement avType, ExecutableElement avConstructor,
          Map<String, ExecutableElement> getters,
          List<TypeVariableName> typevars) {
      this.sourceClass = sourceClass;
      this.generatedClass = generatedClass;
      this.avType = avType;
      this.avConstructor = avConstructor;
      this.getters = getters;
      this.typevars = typevars;
    }

    private static Model create(TypeName sourceClass, TypeElement avType) {
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
  }

  private static ExecutableElement findGetter(VariableElement parameter, Map<String, List<ExecutableElement>> methods) {
    String param = parameter.getSimpleName().toString();
    String upcaseParam = Character.toUpperCase(parameter.getSimpleName().charAt(0)) +
        param.substring(1);
    String[] names = TypeName.get(parameter.asType()).box().equals(TypeName.get(Boolean.class)) ?
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
    String name = String.join("_", rawType(type).simpleNames()) + SUFFIX;
    ClassName className = rawType(type).topLevelClassName().peerClass(name);
    List<TypeName> typeargs = typeArguments(type);
    if (typeargs.isEmpty()) {
      return className;
    }
    return ParameterizedTypeName.get(className, typeargs.toArray(
        new TypeName[typeargs.size()]));
  }

  private static ClassName avPeer(TypeName type) {
    String name = PREFIX + String.join("_", rawType(type).simpleNames());
    return rawType(type).topLevelClassName().peerClass(name);
  }

  private static TypeName maybeParameterized(TypeElement typeElement) {
    if (typeElement.getTypeParameters().isEmpty()) {
      return ClassName.get(typeElement);
    }
    List<TypeVariableName> typevars = typeElement.getTypeParameters().stream()
        .map(TypeVariableName::get)
        .collect(toList());
    TypeVariableName[] typeArguments = typevars.toArray(new TypeVariableName[typevars.size()]);
    return ParameterizedTypeName.get(ClassName.get(typeElement), typeArguments);
  }

  static ClassName rawType(TypeName typeName) {
    if (typeName instanceof ParameterizedTypeName) {
      return ((ParameterizedTypeName) typeName).rawType;
    }
    return ((ClassName) typeName);
  }

  static List<TypeName> typeArguments(TypeName typeName) {
    if (typeName instanceof ParameterizedTypeName) {
      return ((ParameterizedTypeName) typeName).typeArguments;
    }
    return Collections.emptyList();
  }
}
