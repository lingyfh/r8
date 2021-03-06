// Copyright (c) 2021, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.diagnostic;

import com.android.tools.r8.Keep;
import com.android.tools.r8.references.MethodReference;

@Keep
public interface DefinitionMethodContext extends DefinitionContext {

  /** Returns the reference of the method context. */
  MethodReference getMethodReference();

  @Override
  default boolean isMethodContext() {
    return true;
  }

  @Override
  default DefinitionMethodContext asMethodContext() {
    return this;
  }
}
