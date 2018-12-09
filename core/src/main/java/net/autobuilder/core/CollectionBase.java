package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import static net.autobuilder.core.Util.className;

abstract class CollectionBase {

  final String collectionClassName;

  final String overloadArgumentType;

  final Collectionish.CollectionType collectionType;

  abstract CodeBlock accumulatorInitBlock(FieldSpec builderField);

  abstract CodeBlock emptyBlock();

  abstract DeclaredType accumulatorType(Parameter parameter);

  abstract DeclaredType accumulatorOverloadArgumentType(Parameter parameter);

  abstract CodeBlock setterAssignment(Parameter parameter);

  abstract CodeBlock buildBlock(FieldSpec field);

  abstract ParameterSpec setterParameter(Parameter parameter);

  CollectionBase(String collectionClassName,
                 String overloadArgumentType,
                 Collectionish.CollectionType collectionType) {
    this.collectionClassName = collectionClassName;
    this.overloadArgumentType = overloadArgumentType;
    this.collectionType = collectionType;
  }

  TypeElement overloadArgumentType() {
    return TypeTool.get().getTypeElement(overloadArgumentType);
  }

  ClassName collectionClassName() {
    return className(collectionClassName);
  }
}
