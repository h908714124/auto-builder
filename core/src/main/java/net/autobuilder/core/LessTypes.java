package net.autobuilder.core;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.SimpleTypeVisitor6;

import static net.autobuilder.core.LessElements.asType;

final class LessTypes {

  private static final TypeVisitor<Element, Void> AS_ELEMENT_VISITOR =
      new SimpleTypeVisitor6<Element, Void>() {
        @Override
        protected Element defaultAction(TypeMirror e, Void p) {
          return null;
        }

        @Override
        public Element visitDeclared(DeclaredType t, Void p) {
          return t.asElement();
        }

        @Override
        public Element visitError(ErrorType t, Void p) {
          return t.asElement();
        }

        @Override
        public Element visitTypeVariable(TypeVariable t, Void p) {
          return t.asElement();
        }
      };

  static TypeElement asTypeElement(TypeMirror mirror) {
    Element element = asElement(mirror);
    if (element == null) {
      throw new IllegalArgumentException("not an element: " + mirror);
    }
    return asType(element);
  }

  private static Element asElement(TypeMirror typeMirror) {
    return typeMirror.accept(AS_ELEMENT_VISITOR, null);
  }

  private LessTypes() {
    throw new UnsupportedOperationException("no instances");
  }
}
