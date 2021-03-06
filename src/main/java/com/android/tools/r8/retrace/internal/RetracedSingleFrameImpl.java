// Copyright (c) 2021, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.retrace.internal;

import com.android.tools.r8.retrace.RetracedMethodReference;
import com.android.tools.r8.retrace.RetracedSingleFrame;

public class RetracedSingleFrameImpl implements RetracedSingleFrame {

  private final RetracedMethodReference methodReference;
  private final int index;

  private RetracedSingleFrameImpl(RetracedMethodReference methodReference, int index) {
    this.methodReference = methodReference;
    this.index = index;
  }

  @Override
  public RetracedMethodReference getMethodReference() {
    return methodReference;
  }

  @Override
  public int getIndex() {
    return index;
  }

  static RetracedSingleFrameImpl create(RetracedMethodReference methodReference, int index) {
    return new RetracedSingleFrameImpl(methodReference, index);
  }
}
