package net.autobuilder.core;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;
import java.util.Optional;

import static javax.lang.model.element.Modifier.FINAL;

class TypeTool {

  private final TypeVisitor<TypeMirror, Void> subtype =

      new SimpleTypeVisitor8<TypeMirror, Void>() {
        @Override
        public TypeMirror visitDeclared(DeclaredType declaredType, Void _null) {
          if (declaredType.asElement().getModifiers().contains(FINAL)) {
            return declaredType;
          }
          return types.getWildcardType(declaredType, null);
        }

        @Override
        public TypeMirror visitTypeVariable(TypeVariable typeVariable, Void _null) {
          return types.getWildcardType(typeVariable, null);
        }

        @Override
        protected TypeMirror defaultAction(TypeMirror mirror, Void aVoid) {
          return mirror;
        }
      };

  private final ElementVisitor<Optional<TypeElement>, Void> asTypeElement =
      new SimpleElementVisitor8<Optional<TypeElement>, Void>() {
        @Override
        public Optional<TypeElement> visitType(TypeElement e, Void aVoid) {
          return Optional.of(e);
        }

        @Override
        protected Optional<TypeElement> defaultAction(Element e, Void aVoid) {
          return Optional.empty();
        }
      };

  private static TypeTool INSTANCE;

  private final Types types;
  private final Elements elements;

  private TypeTool(Types types, Elements elements) {
    this.types = types;
    this.elements = elements;
  }

  static void init(Types types, Elements elements) {
    INSTANCE = new TypeTool(types, elements);
  }

  static void clear() {
    INSTANCE = null;
  }

  static TypeTool get() {
    return INSTANCE;
  }

  TypeMirror asExtendsWildcard(TypeMirror typeMirror) {
    return typeMirror.accept(subtype, null);
  }

  TypeElement getTypeElement(String qualifiedName) {
    return elements.getTypeElement(qualifiedName);
  }

  Optional<TypeElement> getTypeElement(TypeMirror typeMirror) {
    Element element = types.asElement(typeMirror);
    if (element == null) {
      return Optional.empty();
    }
    return element.accept(asTypeElement, null);
  }
}
