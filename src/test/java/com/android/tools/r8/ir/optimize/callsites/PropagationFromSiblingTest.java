// Copyright (c) 2020, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.ir.optimize.callsites;

import com.android.tools.r8.NeverClassInline;
import com.android.tools.r8.NeverInline;
import com.android.tools.r8.NoVerticalClassMerging;
import com.android.tools.r8.TestBase;
import com.android.tools.r8.TestParameters;
import com.android.tools.r8.utils.BooleanUtils;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PropagationFromSiblingTest extends TestBase {

  private final boolean enableExperimentalArgumentPropagation;
  private final TestParameters parameters;

  @Parameters(name = "{1}, experimental: {0}")
  public static List<Object[]> data() {
    return buildParameters(
        BooleanUtils.values(), getTestParameters().withAllRuntimesAndApiLevels().build());
  }

  public PropagationFromSiblingTest(
      boolean enableExperimentalArgumentPropagation, TestParameters parameters) {
    this.enableExperimentalArgumentPropagation = enableExperimentalArgumentPropagation;
    this.parameters = parameters;
  }

  @Test
  public void test() throws Exception {
    testForR8(parameters.getBackend())
        .addInnerClasses(PropagationFromSiblingTest.class)
        .addKeepMainRule(TestClass.class)
        .addOptionsModification(
            options -> {
              options.enableUnusedInterfaceRemoval = false;
              options
                  .callSiteOptimizationOptions()
                  .setEnableExperimentalArgumentPropagation(enableExperimentalArgumentPropagation);
            })
        .enableInliningAnnotations()
        .enableNoVerticalClassMergingAnnotations()
        .enableNeverClassInliningAnnotations()
        .setMinApi(parameters.getApiLevel())
        .compile()
        .run(parameters.getRuntime(), TestClass.class)
        .assertSuccessWithOutputLines("Hello world!");
  }

  static class TestClass {

    public static void main(String[] args) {
      getJAsI().m(null); // The null argument is not propagated to A.m() in 2.0.59.
      new B().m("Hello world!");
    }

    @NeverInline
    static I getJAsI() {
      if (System.currentTimeMillis() > 0) {
        return new B();
      }
      return new C();
    }
  }

  interface I {
    void m(Object o);
  }

  interface J extends I {}

  @NoVerticalClassMerging
  static class A implements I {

    @NeverInline
    @Override
    public void m(Object o) {
      if (o != null) {
        System.out.println(o.toString());
      }
    }
  }

  @NeverClassInline
  static class B extends A implements J {}

  static class C implements J {

    @Override
    public void m(Object o) {}
  }
}
