package net.autobuilder.core;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import static net.autobuilder.core.Util.className;

public abstract class CollectionBase {

  final String collectionClassName;

  final String overloadArgumentType;

  final CollectionParameter.CollectionType collectionType;

  abstract CodeBlock accumulatorInitBlock(FieldSpec builderField);

  public abstract CodeBlock emptyBlock();

  public abstract DeclaredType accumulatorType(RegularParameter parameter);

  abstract DeclaredType accumulatorOverloadArgumentType(RegularParameter parameter);

  public abstract CodeBlock setterAssignmentCode(RegularParameter parameter);

  public abstract CodeBlock buildBlock(FieldSpec field);

  public abstract ParameterSpec setterParameter(RegularParameter parameter);

  CollectionBase(String collectionClassName,
                 String overloadArgumentType,
                 CollectionParameter.CollectionType collectionType) {
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
