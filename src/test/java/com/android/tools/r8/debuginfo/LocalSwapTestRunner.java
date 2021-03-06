// Copyright (c) 2017, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package com.android.tools.r8.debuginfo;

import static org.junit.Assert.assertEquals;

import com.android.tools.r8.utils.AndroidApp;
import org.junit.Test;

public class LocalSwapTestRunner extends DebugInfoTestBase {

  @Test
  public void testLocalSwap() throws Exception {
    Class clazz = LocalSwapTest.class;
    AndroidApp d8App = compileWithD8(clazz);

    String expected = "6";
    assertEquals(expected, runOnJava(clazz));
    assertEquals(expected, runOnArt(d8App, clazz.getCanonicalName()));

    checkFoo(inspectMethod(d8App, clazz, "int", "foo", "int", "int"));
  }

  private void checkFoo(DebugInfoInspector info) {
    info.checkStartLine(9);
    info.checkLineHasExactLocals(9, "x", "int", "y", "int");
    info.checkLineHasExactLocals(11, "x", "int", "y", "int", "sum", "int");
    info.checkLineHasExactLocals(12, "x", "int", "y", "int", "sum", "int", "t", "int");
    info.checkLineExists(13);
    info.checkLineExists(15);
    info.checkLineHasExactLocals(15, "x", "int", "y", "int", "sum", "int");
  }
}
