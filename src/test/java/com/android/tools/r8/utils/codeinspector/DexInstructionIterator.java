// Copyright (c) 2018, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.utils.codeinspector;

import com.android.tools.r8.code.Instruction;
import com.android.tools.r8.code.SwitchPayload;
import com.android.tools.r8.graph.Code;
import com.android.tools.r8.graph.DexCode;
import com.android.tools.r8.ir.conversion.SwitchPayloadResolver;
import java.util.NoSuchElementException;

class DexInstructionIterator implements InstructionIterator {

  private final CodeInspector codeInspector;
  private final DexCode code;
  private final MethodSubject methodSubject;
  private SwitchPayloadResolver switchPayloadResolver;
  private int index;

  DexInstructionIterator(CodeInspector codeInspector, MethodSubject method) {
    this.codeInspector = codeInspector;
    this.methodSubject = method;
    assert method.isPresent();
    Code code = method.getMethod().getCode();
    assert code != null && code.isDexCode();
    this.code = code.asDexCode();
    this.index = 0;
  }

  @Override
  public boolean hasNext() {
    return index < code.instructions.length;
  }

  @Override
  public InstructionSubject next() {
    if (index == code.instructions.length) {
      throw new NoSuchElementException();
    }
    if (code.instructions[index].isIntSwitch()) {
      ensureSwitchPayloadResolver();
    }
    return codeInspector.createInstructionSubject(
        code.instructions[index++], methodSubject, switchPayloadResolver);
  }

  private void ensureSwitchPayloadResolver() {
    if (switchPayloadResolver == null) {
      switchPayloadResolver = new SwitchPayloadResolver();
      for (Instruction instruction : code.instructions) {
        if (instruction.isIntSwitch()) {
          switchPayloadResolver.addPayloadUser(instruction);
        }
        if (instruction.isSwitchPayload()) {
          switchPayloadResolver.resolve((SwitchPayload) instruction);
        }
      }
    }
  }
}
