// Copyright (c) 2021, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.utils;

import com.android.tools.r8.graph.ProgramMethod;

public class AndroidApiLevelUtils {

  public static OptionalBool isApiSafeForInlining(
      ProgramMethod caller, ProgramMethod inlinee, InternalOptions options) {
    if (!options.apiModelingOptions().enableApiCallerIdentification) {
      return OptionalBool.TRUE;
    }
    if (caller.getHolderType() == inlinee.getHolderType()) {
      return OptionalBool.TRUE;
    }
    return OptionalBool.of(
        caller
            .getDefinition()
            .getApiLevel()
            .isGreaterThanOrEqualTo(inlinee.getDefinition().getApiLevelForCode()));
  }
}
