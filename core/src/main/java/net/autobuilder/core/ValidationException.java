package net.autobuilder.core;

import javax.lang.model.element.Element;

final class ValidationException extends RuntimeException {
  final Element about;

  ValidationException(String message, Element about) {
    super(message);
    this.about = about;
  }
}
