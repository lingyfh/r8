// Copyright (c) 2020, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package com.android.tools.r8.synthesis;

import static com.android.tools.r8.utils.AndroidApiLevel.NOT_SET;

import com.android.tools.r8.cf.CfVersion;
import com.android.tools.r8.graph.Code;
import com.android.tools.r8.graph.DexAnnotationSet;
import com.android.tools.r8.graph.DexEncodedMethod;
import com.android.tools.r8.graph.DexItemFactory;
import com.android.tools.r8.graph.DexMethod;
import com.android.tools.r8.graph.DexProto;
import com.android.tools.r8.graph.DexString;
import com.android.tools.r8.graph.DexType;
import com.android.tools.r8.graph.GenericSignature.MethodTypeSignature;
import com.android.tools.r8.graph.MethodAccessFlags;
import com.android.tools.r8.graph.ParameterAnnotationsList;
import com.android.tools.r8.ir.optimize.info.DefaultMethodOptimizationInfo;
import com.android.tools.r8.ir.optimize.info.MethodOptimizationInfo;
import com.android.tools.r8.synthesis.SyntheticNaming.SyntheticKind;
import com.android.tools.r8.utils.AndroidApiLevel;

public class SyntheticMethodBuilder {

  public interface SyntheticCodeGenerator {
    Code generate(DexMethod method);
  }

  private final DexItemFactory factory;
  private final DexType holderType;
  private final SyntheticKind syntheticKind;
  private DexString name = null;
  private DexProto proto = null;
  private CfVersion classFileVersion;
  private SyntheticCodeGenerator codeGenerator = null;
  private MethodAccessFlags accessFlags = null;
  private MethodTypeSignature genericSignature = MethodTypeSignature.noSignature();
  private DexAnnotationSet annotations = DexAnnotationSet.empty();
  private ParameterAnnotationsList parameterAnnotationsList = ParameterAnnotationsList.empty();
  private AndroidApiLevel apiLevelForDefinition = NOT_SET;
  private AndroidApiLevel apiLevelForCode = NOT_SET;
  private MethodOptimizationInfo optimizationInfo = DefaultMethodOptimizationInfo.getInstance();

  private boolean checkAndroidApiLevels = true;

  SyntheticMethodBuilder(SyntheticClassBuilder<?, ?> parent) {
    this.factory = parent.getFactory();
    this.holderType = parent.getType();
    this.syntheticKind = parent.getSyntheticKind();
  }

  SyntheticMethodBuilder(DexItemFactory factory, DexType holderType, SyntheticKind syntheticKind) {
    this.factory = factory;
    this.holderType = holderType;
    this.syntheticKind = syntheticKind;
  }

  public SyntheticMethodBuilder setName(String name) {
    return setName(factory.createString(name));
  }

  public SyntheticMethodBuilder setName(DexString name) {
    assert name != null;
    assert this.name == null || this.name == name;
    this.name = name;
    return this;
  }

  public SyntheticMethodBuilder setOptimizationInfo(MethodOptimizationInfo optimizationInfo) {
    this.optimizationInfo = optimizationInfo;
    return this;
  }

  public SyntheticMethodBuilder setProto(DexProto proto) {
    this.proto = proto;
    return this;
  }

  public SyntheticMethodBuilder setClassFileVersion(CfVersion classFileVersion) {
    this.classFileVersion = classFileVersion;
    return this;
  }

  public SyntheticMethodBuilder setCode(SyntheticCodeGenerator codeGenerator) {
    this.codeGenerator = codeGenerator;
    return this;
  }

  public SyntheticMethodBuilder setAccessFlags(MethodAccessFlags accessFlags) {
    this.accessFlags = accessFlags;
    return this;
  }

  public SyntheticMethodBuilder setGenericSignature(MethodTypeSignature genericSignature) {
    this.genericSignature = genericSignature;
    return this;
  }

  public SyntheticMethodBuilder setAnnotations(DexAnnotationSet annotations) {
    this.annotations = annotations;
    return this;
  }

  public SyntheticMethodBuilder setParameterAnnotationsList(
      ParameterAnnotationsList parameterAnnotationsList) {
    this.parameterAnnotationsList = parameterAnnotationsList;
    return this;
  }

  public SyntheticMethodBuilder setApiLevelForDefinition(AndroidApiLevel apiLevelForDefinition) {
    this.apiLevelForDefinition = apiLevelForDefinition;
    return this;
  }

  public SyntheticMethodBuilder setApiLevelForCode(AndroidApiLevel apiLevelForCode) {
    this.apiLevelForCode = apiLevelForCode;
    return this;
  }

  public SyntheticMethodBuilder disableAndroidApiLevelCheck() {
    checkAndroidApiLevels = false;
    return this;
  }

  DexEncodedMethod build() {
    assert name != null;
    DexMethod methodSignature = getMethodSignature();
    MethodAccessFlags accessFlags = getAccessFlags();
    Code code = accessFlags.isAbstract() ? null : getCodeObject(methodSignature);
    DexEncodedMethod method =
        DexEncodedMethod.syntheticBuilder()
            .setMethod(methodSignature)
            .setAccessFlags(accessFlags)
            .setGenericSignature(genericSignature)
            .setAnnotations(annotations)
            .setParameterAnnotations(parameterAnnotationsList)
            .setCode(code)
            .setClassFileVersion(classFileVersion)
            .setApiLevelForDefinition(apiLevelForDefinition)
            .setApiLevelForCode(apiLevelForCode)
            .setOptimizationInfo(optimizationInfo)
            .applyIf(!checkAndroidApiLevels, DexEncodedMethod.Builder::disableAndroidApiLevelCheck)
            .build();
    assert isValidSyntheticMethod(method, syntheticKind);
    return method;
  }

  /**
   * Predicate for what is a "supported" synthetic method.
   *
   * <p>This method is used when identifying synthetic methods in the program input and should be as
   * narrow as possible.
   *
   * <p>Methods in fixed suffix synthetics are identified differently (through the class name) and
   * can have different properties.
   */
  public static boolean isValidSyntheticMethod(
      DexEncodedMethod method, SyntheticKind syntheticKind) {
    return isValidSingleSyntheticMethod(method) || syntheticKind.isFixedSuffixSynthetic;
  }

  public static boolean isValidSingleSyntheticMethod(DexEncodedMethod method) {
    return method.isStatic()
        && method.isNonAbstractNonNativeMethod()
        && method.isPublic()
        && method.annotations().isEmpty()
        && method.getParameterAnnotations().isEmpty();
  }

  private DexMethod getMethodSignature() {
    return factory.createMethod(holderType, proto, name);
  }

  private MethodAccessFlags getAccessFlags() {
    return accessFlags;
  }

  private Code getCodeObject(DexMethod methodSignature) {
    return codeGenerator.generate(methodSignature);
  }
}
