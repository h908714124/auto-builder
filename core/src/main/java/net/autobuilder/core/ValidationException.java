package net.autobuilder.core;

import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

final class ValidationException extends RuntimeException {
  private static final long serialVersionUID = 0;
  final Diagnostic.Kind kind;
  final Element about;

  ValidationException(Diagnostic.Kind kind, String message, Element about) {
    super(message);
    this.kind = kind;
    this.about = about;
  }
}
