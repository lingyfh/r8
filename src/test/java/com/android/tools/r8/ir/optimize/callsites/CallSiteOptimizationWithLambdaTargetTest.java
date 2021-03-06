// Copyright (c) 2020, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.ir.optimize.callsites;

import com.android.tools.r8.NeverClassInline;
import com.android.tools.r8.NeverInline;
import com.android.tools.r8.TestBase;
import com.android.tools.r8.TestParameters;
import com.android.tools.r8.utils.BooleanUtils;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CallSiteOptimizationWithLambdaTargetTest extends TestBase {

  private final boolean enableExperimentalArgumentPropagation;
  private final TestParameters parameters;

  @Parameters(name = "{1}, experimental: {0}")
  public static List<Object[]> data() {
    return buildParameters(
        BooleanUtils.values(), getTestParameters().withAllRuntimesAndApiLevels().build());
  }

  public CallSiteOptimizationWithLambdaTargetTest(
      boolean enableExperimentalArgumentPropagation, TestParameters parameters) {
    this.enableExperimentalArgumentPropagation = enableExperimentalArgumentPropagation;
    this.parameters = parameters;
  }

  @Test
  public void test() throws Exception {
    testForR8(parameters.getBackend())
        .addInnerClasses(CallSiteOptimizationWithLambdaTargetTest.class)
        .addKeepMainRule(TestClass.class)
        .addOptionsModification(
            options ->
                options
                    .callSiteOptimizationOptions()
                    .setEnableExperimentalArgumentPropagation(
                        enableExperimentalArgumentPropagation))
        .enableInliningAnnotations()
        .enableNeverClassInliningAnnotations()
        .setMinApi(parameters.getApiLevel())
        .run(parameters.getRuntime(), TestClass.class)
        .assertSuccessWithOutputLines("true", "false");
  }

  static class TestClass {

    public static void main(String[] args) {
      new A().m(new Object());
      get().m(null);
    }

    static I get() {
      return System.currentTimeMillis() >= 0 ? new A() : System.out::println;
    }
  }

  interface I {

    void m(Object o);
  }

  @NeverClassInline
  static class A implements I {

    @NeverInline
    @Override
    public void m(Object o) {
      System.out.println(o != null);
    }
  }
}
