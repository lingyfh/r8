// Copyright (c) 2021, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.graph;

import static com.android.tools.r8.graph.GenericSignature.getEmptyTypeArguments;

import com.android.tools.r8.graph.GenericSignature.ClassSignature;
import com.android.tools.r8.graph.GenericSignature.ClassTypeSignature;
import com.android.tools.r8.graph.GenericSignature.FieldTypeSignature;
import com.android.tools.r8.graph.GenericSignature.FormalTypeParameter;
import com.android.tools.r8.graph.GenericSignature.MethodTypeSignature;
import com.android.tools.r8.graph.GenericSignature.ReturnType;
import com.android.tools.r8.graph.GenericSignature.TypeSignature;
import com.android.tools.r8.graph.GenericSignature.WildcardIndicator;
import com.android.tools.r8.graph.GenericSignatureContextBuilder.TypeParameterContext;
import com.android.tools.r8.utils.ListUtils;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class GenericSignaturePartialTypeArgumentApplier implements GenericSignatureVisitor {

  private final TypeParameterContext typeParameterContext;
  private final BiPredicate<DexType, DexType> enclosingPruned;
  private final Predicate<DexType> hasGenericTypeParameters;
  private final AppView<?> appView;

  private GenericSignaturePartialTypeArgumentApplier(
      AppView<?> appView,
      TypeParameterContext typeParameterContext,
      BiPredicate<DexType, DexType> enclosingPruned,
      Predicate<DexType> hasGenericTypeParameters) {
    this.appView = appView;
    this.typeParameterContext = typeParameterContext;
    this.enclosingPruned = enclosingPruned;
    this.hasGenericTypeParameters = hasGenericTypeParameters;
  }

  public static GenericSignaturePartialTypeArgumentApplier build(
      AppView<?> appView,
      TypeParameterContext typeParameterContext,
      BiPredicate<DexType, DexType> enclosingPruned,
      Predicate<DexType> hasGenericTypeParameters) {
    return new GenericSignaturePartialTypeArgumentApplier(
        appView, typeParameterContext, enclosingPruned, hasGenericTypeParameters);
  }

  public GenericSignaturePartialTypeArgumentApplier buildForMethod(
      List<FormalTypeParameter> formals) {
    if (formals.isEmpty()) {
      return this;
    }
    return new GenericSignaturePartialTypeArgumentApplier(
        appView,
        typeParameterContext.addLiveParameters(
            ListUtils.map(formals, FormalTypeParameter::getName)),
        enclosingPruned,
        hasGenericTypeParameters);
  }

  @Override
  public ClassSignature visitClassSignature(ClassSignature classSignature) {
    if (classSignature.hasNoSignature() || classSignature.isInvalid()) {
      return classSignature;
    }
    return classSignature.visit(this);
  }

  @Override
  public MethodTypeSignature visitMethodSignature(MethodTypeSignature methodSignature) {
    if (methodSignature.hasNoSignature() || methodSignature.isInvalid()) {
      return methodSignature;
    }
    return methodSignature.visit(this);
  }

  @Override
  public DexType visitType(DexType type) {
    return type;
  }

  @Override
  public TypeSignature visitTypeSignature(TypeSignature typeSignature) {
    if (typeSignature.isBaseTypeSignature()) {
      return typeSignature;
    }
    return visitFieldTypeSignature(typeSignature.asFieldTypeSignature());
  }

  @Override
  public FormalTypeParameter visitFormalTypeParameter(FormalTypeParameter formalTypeParameter) {
    FormalTypeParameter rewritten = formalTypeParameter.visit(this);
    // Guard against no information being present in bounds.
    assert (rewritten.getClassBound() != null && rewritten.getClassBound().hasSignature())
        || !rewritten.getInterfaceBounds().isEmpty();
    return rewritten;
  }

  @Override
  public List<FieldTypeSignature> visitInterfaceBounds(List<FieldTypeSignature> fieldSignatures) {
    if (fieldSignatures == null || fieldSignatures.isEmpty()) {
      return fieldSignatures;
    }
    return ListUtils.mapOrElse(fieldSignatures, this::visitFieldTypeSignature);
  }

  @Override
  public List<ClassTypeSignature> visitSuperInterfaces(
      List<ClassTypeSignature> interfaceSignatures) {
    if (interfaceSignatures.isEmpty()) {
      return interfaceSignatures;
    }
    return ListUtils.mapOrElse(interfaceSignatures, this::visitSuperInterface);
  }

  @Override
  public List<FieldTypeSignature> visitTypeArguments(
      DexType type, List<FieldTypeSignature> typeArguments) {
    if (typeArguments.isEmpty() || !hasGenericTypeParameters.test(type)) {
      return getEmptyTypeArguments();
    }
    return ListUtils.mapOrElse(typeArguments, this::visitFieldTypeSignature);
  }

  @Override
  public ClassTypeSignature visitSuperInterface(ClassTypeSignature classTypeSignature) {
    return classTypeSignature.visit(this);
  }

  @Override
  public FieldTypeSignature visitClassBound(FieldTypeSignature fieldSignature) {
    return visitFieldTypeSignature(fieldSignature);
  }

  @Override
  public FieldTypeSignature visitInterfaceBound(FieldTypeSignature fieldSignature) {
    return visitFieldTypeSignature(fieldSignature);
  }

  @Override
  public ClassTypeSignature visitEnclosing(
      ClassTypeSignature enclosingSignature, ClassTypeSignature enclosedSignature) {
    if (enclosingPruned.test(enclosingSignature.type(), enclosedSignature.type())) {
      return null;
    } else {
      return enclosingSignature.visit(this);
    }
  }

  @Override
  public List<TypeSignature> visitThrowsSignatures(List<TypeSignature> typeSignatures) {
    if (typeSignatures.isEmpty()) {
      return typeSignatures;
    }
    return ListUtils.mapOrElse(typeSignatures, this::visitTypeSignature);
  }

  @Override
  public ReturnType visitReturnType(ReturnType returnType) {
    if (returnType.isVoidDescriptor()) {
      return returnType;
    }
    TypeSignature originalSignature = returnType.typeSignature;
    TypeSignature rewrittenSignature = visitTypeSignature(originalSignature);
    if (originalSignature == rewrittenSignature) {
      return returnType;
    }
    return new ReturnType(rewrittenSignature);
  }

  @Override
  public List<FormalTypeParameter> visitFormalTypeParameters(
      List<FormalTypeParameter> formalTypeParameters) {
    if (formalTypeParameters.isEmpty()) {
      return formalTypeParameters;
    }
    return ListUtils.mapOrElse(formalTypeParameters, this::visitFormalTypeParameter);
  }

  @Override
  public List<TypeSignature> visitMethodTypeSignatures(List<TypeSignature> typeSignatures) {
    if (typeSignatures.isEmpty()) {
      return typeSignatures;
    }
    return ListUtils.mapOrElse(typeSignatures, this::visitTypeSignature);
  }

  @Override
  public ClassTypeSignature visitSuperClass(ClassTypeSignature classTypeSignature) {
    return classTypeSignature.visit(this);
  }

  @Override
  public FieldTypeSignature visitFieldTypeSignature(FieldTypeSignature fieldSignature) {
    if (fieldSignature.hasNoSignature() || fieldSignature.isInvalid()) {
      return fieldSignature;
    }
    if (fieldSignature.isStar()) {
      return fieldSignature;
    } else if (fieldSignature.isClassTypeSignature()) {
      return fieldSignature.asClassTypeSignature().visit(this);
    } else if (fieldSignature.isArrayTypeSignature()) {
      return fieldSignature.asArrayTypeSignature().visit(this);
    } else {
      assert fieldSignature.isTypeVariableSignature();
      String typeVariableName = fieldSignature.asTypeVariableSignature().typeVariable();
      if (!typeParameterContext.isLiveParameter(typeVariableName)) {
        DexType substitution = typeParameterContext.getPrunedSubstitution(typeVariableName);
        if (substitution == null) {
          substitution = appView.dexItemFactory().objectType;
        }
        return new ClassTypeSignature(substitution).asArgument(WildcardIndicator.NONE);
      }
      return fieldSignature;
    }
  }
}
