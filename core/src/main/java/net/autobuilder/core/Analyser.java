package net.autobuilder.core;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.List;

final class Analyser {

  private final Processor.Model model;

  private Analyser(Processor.Model model) {
    this.model = model;
  }

  static Analyser create(Processor.Model model) {
    return new Analyser(model);
  }

  TypeSpec analyse() {
    TypeSpec.Builder builder = TypeSpec.classBuilder(model.generatedClass);
    List<ExecutableElement> constructors = ElementFilter.constructorsIn(model.avType.getEnclosedElements());
    if (constructors.size() != 1) {
      throw new ValidationException(Diagnostic.Kind.ERROR, "Expecting exactly one constructor", model.avType);
    }
    for (VariableElement variableElement : constructors.get(0).getParameters()) {
      builder.addField(FieldSpec.builder(TypeName.get(variableElement.asType()), variableElement.getSimpleName().toString())
          .addModifiers(Modifier.PRIVATE)
          .build());
    }
    return builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL).build();
  }
}
