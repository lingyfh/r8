// Copyright (c) 2020, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.ir.optimize.switches;

import static com.android.tools.r8.utils.codeinspector.Matchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.android.tools.r8.TestBase;
import com.android.tools.r8.TestParameters;
import com.android.tools.r8.TestParametersCollection;
import com.android.tools.r8.graph.DexItemFactory;
import com.android.tools.r8.ir.code.IRCode;
import com.android.tools.r8.ir.code.Instruction;
import com.android.tools.r8.utils.InternalOptions;
import com.android.tools.r8.utils.Reporter;
import com.android.tools.r8.utils.codeinspector.ClassSubject;
import com.android.tools.r8.utils.codeinspector.CodeInspector;
import com.android.tools.r8.utils.codeinspector.InstructionSubject;
import com.android.tools.r8.utils.codeinspector.InstructionSubject.JumboStringMode;
import com.android.tools.r8.utils.codeinspector.MethodSubject;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ConvertRemovedStringSwitchTest extends TestBase {

  private final TestParameters parameters;

  @Parameterized.Parameters(name = "{0}")
  public static TestParametersCollection data() {
    return getTestParameters().withAllRuntimesAndApiLevels().build();
  }

  public ConvertRemovedStringSwitchTest(TestParameters parameters) {
    this.parameters = parameters;
  }

  // TODO(b/135721688): We only introduce string-switches when there is a comparison to the hash
  //  code of a string. Thus, we won't be able to recognize the string-switch in the output until we
  //  have a hash-based string-switch elimination.
  @Ignore("b/135721688")
  @Test
  public void test() throws Exception {
    testForR8(parameters.getBackend())
        .addInnerClasses(ConvertRemovedStringSwitchTest.class)
        .addKeepMainRule(TestClass.class)
        .addOptionsModification(
            options -> {
              assert !options.enableStringSwitchConversion;
              options.enableStringSwitchConversion = true;
            })
        .setMinApi(parameters.getApiLevel())
        .compile()
        .inspect(this::inspect)
        .run(parameters.getRuntime(), TestClass.class, "A", "B", "C", "D", "E")
        .assertSuccessWithOutputLines("A", "B", "C", "D", "E!");
  }

  private void inspect(CodeInspector inspector) {
    ClassSubject classSubject = inspector.clazz(TestClass.class);
    assertThat(classSubject, isPresent());

    MethodSubject mainMethodSubject = classSubject.mainMethod();
    assertThat(mainMethodSubject, isPresent());

    DexItemFactory dexItemFactory = new DexItemFactory();
    InternalOptions options = new InternalOptions(dexItemFactory, new Reporter());
    assert !options.enableStringSwitchConversion;
    options.enableStringSwitchConversion = true;

    // Verify that the keys were canonicalized.
    Reference2IntMap<String> stringCounts = countStrings(mainMethodSubject);
    assertEquals(1, stringCounts.getInt("A"));
    assertEquals(1, stringCounts.getInt("B"));
    assertEquals(1, stringCounts.getInt("B"));
    assertEquals(1, stringCounts.getInt("D"));
    assertEquals(1, stringCounts.getInt("E"));
    assertEquals(1, stringCounts.getInt("E!"));

    // Verify that we can rebuild the StringSwitch instruction.
    IRCode code = mainMethodSubject.buildIR(options);
    assertTrue(code.streamInstructions().anyMatch(Instruction::isStringSwitch));
  }

  private static Reference2IntMap<String> countStrings(MethodSubject methodSubject) {
    Reference2IntMap<String> result = new Reference2IntOpenHashMap<>();
    methodSubject
        .streamInstructions()
        .filter(instruction -> instruction.isConstString(JumboStringMode.ALLOW))
        .map(InstructionSubject::getConstString)
        .forEach(string -> result.put(string, result.getInt(string) + 1));
    return result;
  }

  static class TestClass {

    public static void main(String[] args) {
      for (String arg : args) {
        switch (arg) {
          case "A":
            System.out.println("A");
            break;
          case "B":
            System.out.println("B");
            break;
          case "C":
            System.out.println("C");
            break;
          case "D":
            System.out.println("D");
            break;
          case "E":
            // Intentionally "E!" to prevent canonicalization of this key.
            System.out.println("E!");
            break;
        }
      }
    }
  }
}