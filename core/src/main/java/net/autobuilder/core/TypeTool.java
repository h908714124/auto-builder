package net.autobuilder.core;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;
import java.util.List;
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

  private final TypeVisitor<Boolean, Void> isWild =

      new SimpleTypeVisitor8<Boolean, Void>() {

        @Override
        public Boolean visitWildcard(WildcardType t, Void aVoid) {
          return t.getExtendsBound() != null || t.getSuperBound() != null;
        }

        @Override
        protected Boolean defaultAction(TypeMirror e, Void aVoid) {
          return false;
        }
      };

  private final TypeVisitor<DeclaredType, Void> declared =

      new SimpleTypeVisitor8<DeclaredType, Void>() {
        @Override
        public DeclaredType visitDeclared(DeclaredType declaredType, Void _null) {
          return declaredType;
        }

        @Override
        protected DeclaredType defaultAction(TypeMirror mirror, Void _null) {
          throw new AssertionError("expecting declared type but was " + mirror);
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

  DeclaredType getDeclaredType(String qualifiedName, List<? extends TypeMirror> mirrors) {
    TypeElement typeElement = getTypeElement(qualifiedName);
    if (typeElement == null) {
      throw new IllegalStateException("no TypeElement: " + qualifiedName);
    }
    return types.getDeclaredType(typeElement, mirrors.toArray(new TypeMirror[0]));
  }

  DeclaredType getDeclaredType(TypeMirror mirror) {
    return mirror.accept(declared, null);
  }

  DeclaredType getDeclaredType(TypeMirror mirror, TypeMirror... typeargs) {
    Element el = mirror.accept(declared, null).asElement();
    return types.getDeclaredType(el.accept(asTypeElement, null).orElseThrow(IllegalArgumentException::new), typeargs);
  }

  TypeMirror asExtendsWildcard(TypeMirror typeMirror) {
    return typeMirror.accept(subtype, null);
  }

  TypeElement getTypeElement(String qualifiedName) {
    return elements.getTypeElement(qualifiedName);
  }

  TypeElement getTypeElement(Class<?> clazz) {
    return elements.getTypeElement(clazz.getCanonicalName());
  }

  boolean hasWildcards(TypeMirror mirror) {
    if (mirror.accept(isWild, null)) {
      return true;
    }
    if (mirror.getKind() != TypeKind.DECLARED) {
      return false;
    }
    DeclaredType declared = mirror.accept(this.declared, null);
    for (TypeMirror typeArgument : declared.getTypeArguments()) {
      if (typeArgument.accept(isWild, null)) {
        return true;
      }
    }
    return false;
  }

  Optional<TypeElement> getTypeElement(TypeMirror typeMirror) {
    Element element = types.asElement(typeMirror);
    if (element == null) {
      return Optional.empty();
    }
    return element.accept(asTypeElement, null);
  }

  boolean isSameType(Class<?> m0, TypeMirror m1) {
    return types.isSameType(elements.getTypeElement(m0.getCanonicalName()).asType(), m1);
  }

  boolean isSameErasure(Class<?> m0, TypeMirror m1) {
    return types.isSameType(
        types.erasure(elements.getTypeElement(m0.getCanonicalName()).asType()),
        types.erasure(m1));
  }

  boolean isSameErasure(TypeMirror m0, TypeMirror m1) {
    return types.isSameType(types.erasure(m0), types.erasure(m1));
  }

  TypeMirror getPrimitiveType(TypeKind kind) {
    return types.getPrimitiveType(kind);
  }
}
