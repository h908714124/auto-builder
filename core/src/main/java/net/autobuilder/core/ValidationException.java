package net.autobuilder.core;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

final class ValidationException extends RuntimeException {

  final Element about;
  final Diagnostic.Kind kind;

  private ValidationException(String message, Element about, Diagnostic.Kind kind) {
    super(message);
    this.about = about;
    this.kind = kind;
  }

  ValidationException(String message, Element about) {
    this(message, about, Diagnostic.Kind.ERROR);
  }

}
