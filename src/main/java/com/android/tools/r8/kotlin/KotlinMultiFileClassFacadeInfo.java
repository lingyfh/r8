// Copyright (c) 2020, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.kotlin;

import static com.android.tools.r8.utils.FunctionUtils.forEachApply;

import com.android.tools.r8.graph.AppView;
import com.android.tools.r8.graph.DexClass;
import com.android.tools.r8.graph.DexDefinitionSupplier;
import com.android.tools.r8.graph.DexItemFactory;
import com.android.tools.r8.naming.NamingLens;
import com.android.tools.r8.utils.Pair;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import kotlinx.metadata.jvm.KotlinClassHeader;
import kotlinx.metadata.jvm.KotlinClassMetadata;
import kotlinx.metadata.jvm.KotlinClassMetadata.MultiFileClassFacade;

// Holds information about Metadata.MultiFileClassFace
public class KotlinMultiFileClassFacadeInfo implements KotlinClassLevelInfo {

  private final List<KotlinTypeReference> partClassNames;
  private final String packageName;
  private final int[] metadataVersion;

  private KotlinMultiFileClassFacadeInfo(
      List<KotlinTypeReference> partClassNames, String packageName, int[] metadataVersion) {
    this.partClassNames = partClassNames;
    this.packageName = packageName;
    this.metadataVersion = metadataVersion;
  }

  static KotlinMultiFileClassFacadeInfo create(
      MultiFileClassFacade kmMultiFileClassFacade,
      String packageName,
      int[] metadataVersion,
      DexItemFactory factory) {
    ImmutableList.Builder<KotlinTypeReference> builder = ImmutableList.builder();
    for (String partClassName : kmMultiFileClassFacade.getPartClassNames()) {
      builder.add(KotlinTypeReference.fromBinaryName(partClassName, factory, partClassName));
    }
    return new KotlinMultiFileClassFacadeInfo(builder.build(), packageName, metadataVersion);
  }

  @Override
  public boolean isMultiFileFacade() {
    return true;
  }

  @Override
  public KotlinMultiFileClassFacadeInfo asMultiFileFacade() {
    return this;
  }

  @Override
  public Pair<KotlinClassHeader, Boolean> rewrite(
      DexClass clazz, AppView<?> appView, NamingLens namingLens) {
    List<String> partClassNameStrings = new ArrayList<>(partClassNames.size());
    boolean rewritten = false;
    for (KotlinTypeReference partClassName : partClassNames) {
      rewritten |=
          partClassName.toRenamedBinaryNameOrDefault(
              binaryName -> {
                if (binaryName != null) {
                  partClassNameStrings.add(binaryName);
                }
              },
              appView,
              namingLens,
              null);
    }
    KotlinClassMetadata.MultiFileClassFacade.Writer writer =
        new KotlinClassMetadata.MultiFileClassFacade.Writer();
    return Pair.create(writer.write(partClassNameStrings).getHeader(), rewritten);
  }

  @Override
  public String getPackageName() {
    return packageName;
  }

  @Override
  public int[] getMetadataVersion() {
    return metadataVersion;
  }

  @Override
  public void trace(DexDefinitionSupplier definitionSupplier) {
    forEachApply(partClassNames, type -> type::trace, definitionSupplier);
  }
}
