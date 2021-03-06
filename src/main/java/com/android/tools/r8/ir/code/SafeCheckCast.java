// Copyright (c) 2021, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.ir.code;

import com.android.tools.r8.cf.code.CfSafeCheckCast;
import com.android.tools.r8.graph.AppView;
import com.android.tools.r8.graph.DexType;
import com.android.tools.r8.graph.ProgramMethod;
import com.android.tools.r8.ir.conversion.CfBuilder;

public class SafeCheckCast extends CheckCast {

  public SafeCheckCast(Value dest, Value value, DexType type) {
    super(dest, value, type);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public void buildCf(CfBuilder builder) {
    builder.add(new CfSafeCheckCast(getType()));
  }

  @Override
  com.android.tools.r8.code.CheckCast createCheckCast(int register) {
    return new com.android.tools.r8.code.SafeCheckCast(register, getType());
  }

  @Override
  public boolean instructionInstanceCanThrow(AppView<?> appView, ProgramMethod context) {
    return false;
  }

  public static class Builder extends CheckCast.Builder {

    @Override
    public CheckCast build() {
      return amend(new SafeCheckCast(outValue, object, castType));
    }
  }
}
