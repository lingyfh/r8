// Copyright (c) 2020, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.ir.optimize.inliner;

import static com.android.tools.r8.utils.codeinspector.Matchers.isPresent;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import com.android.tools.r8.LibraryDesugaringTestConfiguration;
import com.android.tools.r8.TestBase;
import com.android.tools.r8.TestParameters;
import com.android.tools.r8.TestParametersCollection;
import com.android.tools.r8.ToolHelper;
import com.android.tools.r8.utils.AndroidApiLevel;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class InlineMethodWithRetargetedLibMemberTest extends TestBase {

  private final TestParameters parameters;

  @Parameters(name = "{0}")
  public static TestParametersCollection data() {
    return TestBase.getTestParameters().withDexRuntimes().withAllApiLevels().build();
  }

  public InlineMethodWithRetargetedLibMemberTest(TestParameters parameters) {
    this.parameters = parameters;
  }

  @Test
  public void test() throws Exception {
    testForR8(parameters.getBackend())
        .addLibraryFiles(ToolHelper.getAndroidJar(AndroidApiLevel.P))
        .addProgramClasses(TestClass.class)
        .addKeepMainRule(TestClass.class)
        .enableCoreLibraryDesugaring(
            LibraryDesugaringTestConfiguration.forApiLevel(parameters.getApiLevel()))
        .setMinApi(parameters.getApiLevel())
        .compile()
        .inspect(
            inspector ->
                assertThat(
                    inspector.clazz(TestClass.class).uniqueMethodWithName("test"),
                    not(isPresent())));
  }

  static class TestClass {

    public static void main(String[] args) {
      test(args);
    }

    static void test(String[] args) {
      Arrays.stream(args);
    }
  }
}
