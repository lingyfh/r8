// Copyright (c) 2020, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package com.android.tools.r8.shaking;

import com.android.tools.r8.graph.DexClass;
import com.android.tools.r8.graph.DexItemFactory;
import com.android.tools.r8.graph.DexProgramClass;
import com.android.tools.r8.graph.DexType;
import com.android.tools.r8.graph.ProgramDefinition;
import com.android.tools.r8.utils.InternalOptions;
import java.util.function.Function;

/** Immutable keep requirements for a class. */
public final class KeepClassInfo extends KeepInfo<KeepClassInfo.Builder, KeepClassInfo> {

  // Requires all aspects of a class to be kept.
  private static final KeepClassInfo TOP = new Builder().makeTop().build();

  // Requires no aspects of a class to be kept.
  private static final KeepClassInfo BOTTOM = new Builder().makeBottom().build();

  public static KeepClassInfo top() {
    return TOP;
  }

  public static KeepClassInfo bottom() {
    return BOTTOM;
  }

  public static Joiner newEmptyJoiner() {
    return bottom().joiner();
  }

  private KeepClassInfo(Builder builder) {
    super(builder);
  }

  @Override
  Builder builder() {
    return new Builder(this);
  }

  public Joiner joiner() {
    assert !isTop();
    return new Joiner(this);
  }

  @Override
  public boolean isRepackagingAllowed(
      ProgramDefinition definition, GlobalKeepInfoConfiguration configuration) {
    return configuration.isRepackagingEnabled()
        && internalIsMinificationAllowed()
        && (definition.getAccessFlags().isPublic()
            || !internalIsAccessModificationRequiredForRepackaging());
  }

  public boolean isKotlinMetadataRemovalAllowed(
      GlobalKeepInfoConfiguration configuration, boolean kotlinMetadataKept) {
    return !kotlinMetadataKept
        || !isPinned(configuration)
        || !configuration.isKeepRuntimeVisibleAnnotationsEnabled()
        || isAnnotationRemovalAllowed(configuration);
  }

  public static boolean isKotlinMetadataClassKept(
      DexItemFactory factory,
      InternalOptions options,
      Function<DexType, DexClass> definitionForWithoutExistenceAssert,
      Function<DexProgramClass, KeepClassInfo> getClassInfo) {
    DexType kotlinMetadataType = factory.kotlinMetadataType;
    DexClass kotlinMetadataClass = definitionForWithoutExistenceAssert.apply(kotlinMetadataType);
    return kotlinMetadataClass == null
        || kotlinMetadataClass.isNotProgramClass()
        || !getClassInfo.apply(kotlinMetadataClass.asProgramClass()).isShrinkingAllowed(options);
  }

  @Override
  public boolean isTop() {
    return this.equals(top());
  }

  @Override
  public boolean isBottom() {
    return this.equals(bottom());
  }

  public static class Builder extends KeepInfo.Builder<Builder, KeepClassInfo> {

    private Builder() {
      super();
    }

    private Builder(KeepClassInfo original) {
      super(original);
    }

    @Override
    public KeepClassInfo getTopInfo() {
      return TOP;
    }

    @Override
    public KeepClassInfo getBottomInfo() {
      return BOTTOM;
    }

    @Override
    public Builder self() {
      return this;
    }

    @Override
    public boolean isEqualTo(KeepClassInfo other) {
      return internalIsEqualTo(other);
    }

    @Override
    public KeepClassInfo doBuild() {
      return new KeepClassInfo(this);
    }
  }

  public static class Joiner extends KeepInfo.Joiner<Joiner, Builder, KeepClassInfo> {

    public Joiner(KeepClassInfo info) {
      super(info.builder());
    }

    @Override
    public Joiner asClassJoiner() {
      return this;
    }

    @Override
    public Joiner merge(Joiner joiner) {
      // Should be extended to merge the fields of this class in case any are added.
      return super.merge(joiner);
    }

    @Override
    Joiner self() {
      return this;
    }
  }
}
