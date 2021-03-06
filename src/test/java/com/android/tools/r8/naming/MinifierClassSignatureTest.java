// Copyright (c) 2018, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.naming;

import static com.android.tools.r8.DiagnosticsMatcher.diagnosticMessage;
import static com.android.tools.r8.DiagnosticsMatcher.diagnosticOrigin;
import static com.android.tools.r8.utils.codeinspector.Matchers.isPresent;
import static com.android.tools.r8.utils.codeinspector.Matchers.isPresentAndRenamed;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

import com.android.tools.r8.R8TestCompileResult;
import com.android.tools.r8.TestBase;
import com.android.tools.r8.TestDiagnosticMessages;
import com.android.tools.r8.TestParameters;
import com.android.tools.r8.TestParametersCollection;
import com.android.tools.r8.origin.Origin;
import com.android.tools.r8.shaking.ProguardKeepAttributes;
import com.android.tools.r8.utils.ThrowingConsumer;
import com.android.tools.r8.utils.codeinspector.ClassSubject;
import com.android.tools.r8.utils.codeinspector.CodeInspector;
import com.google.common.collect.ImmutableMap;
import java.util.function.Consumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

@RunWith(Parameterized.class)
public class MinifierClassSignatureTest extends TestBase {
  /*

  class Simple {
  }
  class Base<T> {
  }
  class Outer<T> {
    class Inner {
      class InnerInner {
      }
      class ExtendsInnerInner extends InnerInner {
      }
    }
    class ExtendsInner extends Inner {
    }
  }

  */

  String baseSignature = "<T:Ljava/lang/Object;>Ljava/lang/Object;";
  String outerSignature = "<T:Ljava/lang/Object;>Ljava/lang/Object;";
  String extendsInnerSignature = "LOuter<TT;>.Inner;";
  String extendsInnerInnerSignature = "LOuter<TT;>.Inner.InnerInner;";

  String extendsInnerSignatureInvalidOuter = "LOuter.Inner;";
  String extendsInnerInnerSignatureInvalidOuter = "LOuter.Inner.InnerInner;";

  private final TestParameters parameters;

  @Parameters(name = "{0}")
  public static TestParametersCollection data() {
    return getTestParameters().withAllRuntimesAndApiLevels().build();
  }

  public MinifierClassSignatureTest(TestParameters parameters) {
    this.parameters = parameters;
  }

  private byte[] dumpSimple(String classSignature) {

    ClassWriter cw = new ClassWriter(0);
    MethodVisitor mv;

    cw.visit(V1_8, ACC_SUPER, "Simple", classSignature, "java/lang/Object", null);

    {
      mv = cw.visitMethod(0, "<init>", "()V", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
      mv.visitInsn(RETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    cw.visitEnd();

    return cw.toByteArray();
  }

  private byte[] dumpBase(String classSignature) {

    final String javacClassSignature = baseSignature;
    ClassWriter cw = new ClassWriter(0);
    MethodVisitor mv;

    String signature = classSignature != null ? classSignature : javacClassSignature;
    cw.visit(V1_8, ACC_SUPER, "Base", signature, "java/lang/Object", null);

    {
      mv = cw.visitMethod(0, "<init>", "()V", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
      mv.visitInsn(RETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    cw.visitEnd();

    return cw.toByteArray();
  }


  private byte[] dumpOuter(String classSignature) {

    final String javacClassSignature = outerSignature;
    ClassWriter cw = new ClassWriter(0);
    MethodVisitor mv;

    String signature = classSignature != null ? classSignature : javacClassSignature;
    cw.visit(V1_8, ACC_SUPER, "Outer", signature, "java/lang/Object", null);

    cw.visitInnerClass("Outer$ExtendsInner", "Outer", "ExtendsInner", 0);

    cw.visitInnerClass("Outer$Inner", "Outer", "Inner", 0);

    {
      mv = cw.visitMethod(0, "<init>", "()V", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
      mv.visitInsn(RETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    cw.visitEnd();

    return cw.toByteArray();
  }

  private byte[] dumpInner(String classSignature) {

    final String javacClassSignature = null;
    ClassWriter cw = new ClassWriter(0);
    FieldVisitor fv;
    MethodVisitor mv;

    String signature = classSignature != null ? classSignature : javacClassSignature;
    cw.visit(V1_8, ACC_SUPER, "Outer$Inner", signature, "java/lang/Object", null);

    cw.visitInnerClass("Outer$Inner", "Outer", "Inner", 0);

    cw.visitInnerClass("Outer$Inner$ExtendsInnerInner", "Outer$Inner", "ExtendsInnerInner", 0);

    cw.visitInnerClass("Outer$Inner$InnerInner", "Outer$Inner", "InnerInner", 0);

    {
      fv = cw.visitField(ACC_FINAL + ACC_SYNTHETIC, "this$0", "LOuter;", null, null);
      fv.visitEnd();
    }
    {
      mv = cw.visitMethod(0, "<init>", "(LOuter;)V", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitFieldInsn(PUTFIELD, "Outer$Inner", "this$0", "LOuter;");
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
      mv.visitInsn(RETURN);
      mv.visitMaxs(2, 2);
      mv.visitEnd();
    }
    cw.visitEnd();

    return cw.toByteArray();
  }

  private byte[] dumpExtendsInner(String classSignature) {

    final String javacClassSignature = extendsInnerSignature;
    ClassWriter cw = new ClassWriter(0);
    FieldVisitor fv;
    MethodVisitor mv;

    String signature = classSignature != null ? classSignature : javacClassSignature;
    cw.visit(V1_8, ACC_SUPER, "Outer$ExtendsInner", signature, "Outer$Inner", null);

    cw.visitInnerClass("Outer$Inner", "Outer", "Inner", 0);

    cw.visitInnerClass("Outer$ExtendsInner", "Outer", "ExtendsInner", 0);

    {
      fv = cw.visitField(ACC_FINAL + ACC_SYNTHETIC, "this$0", "LOuter;", null, null);
      fv.visitEnd();
    }
    {
      mv = cw.visitMethod(0, "<init>", "(LOuter;)V", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitFieldInsn(PUTFIELD, "Outer$ExtendsInner", "this$0", "LOuter;");
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKESPECIAL, "Outer$Inner", "<init>", "(LOuter;)V", false);
      mv.visitInsn(RETURN);
      mv.visitMaxs(2, 2);
      mv.visitEnd();
    }
    cw.visitEnd();

    return cw.toByteArray();
  }

  private byte[] dumpInnerInner(String classSignature) {

    final String javacClassSignature = null;
    ClassWriter cw = new ClassWriter(0);
    FieldVisitor fv;
    MethodVisitor mv;

    String signature = classSignature != null ? classSignature : javacClassSignature;
    cw.visit(V1_8, ACC_SUPER, "Outer$Inner$InnerInner", signature, "java/lang/Object", null);

    cw.visitInnerClass("Outer$Inner", "Outer", "Inner", 0);

    cw.visitInnerClass("Outer$Inner$InnerInner", "Outer$Inner", "InnerInner", 0);

    {
      fv = cw.visitField(ACC_FINAL + ACC_SYNTHETIC, "this$1", "LOuter$Inner;", null, null);
      fv.visitEnd();
    }
    {
      mv = cw.visitMethod(0, "<init>", "(LOuter$Inner;)V", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitFieldInsn(PUTFIELD, "Outer$Inner$InnerInner", "this$1", "LOuter$Inner;");
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
      mv.visitInsn(RETURN);
      mv.visitMaxs(2, 2);
      mv.visitEnd();
    }
    cw.visitEnd();

    return cw.toByteArray();
  }

  private byte[] dumpExtendsInnerInner(String classSignature) {

    final String javacClassSignature = extendsInnerInnerSignature;
    ClassWriter cw = new ClassWriter(0);
    FieldVisitor fv;
    MethodVisitor mv;

    String signature = classSignature != null ? classSignature : javacClassSignature;
    cw.visit(V1_8, ACC_SUPER, "Outer$Inner$ExtendsInnerInner", signature, "Outer$Inner$InnerInner",
        null);

    cw.visitInnerClass("Outer$Inner", "Outer", "Inner", 0);

    cw.visitInnerClass("Outer$Inner$InnerInner", "Outer$Inner", "InnerInner", 0);

    cw.visitInnerClass("Outer$Inner$ExtendsInnerInner", "Outer$Inner", "ExtendsInnerInner", 0);

    {
      fv = cw.visitField(ACC_FINAL + ACC_SYNTHETIC, "this$1", "LOuter$Inner;", null, null);
      fv.visitEnd();
    }
    {
      mv = cw.visitMethod(0, "<init>", "(LOuter$Inner;)V", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitFieldInsn(PUTFIELD, "Outer$Inner$ExtendsInnerInner", "this$1", "LOuter$Inner;");
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitMethodInsn(INVOKESPECIAL, "Outer$Inner$InnerInner", "<init>", "(LOuter$Inner;)V",
          false);
      mv.visitInsn(RETURN);
      mv.visitMaxs(2, 2);
      mv.visitEnd();
    }
    cw.visitEnd();

    return cw.toByteArray();
  }

  public void runTest(
      ImmutableMap<String, String> signatures,
      Consumer<TestDiagnosticMessages> diagnostics,
      ThrowingConsumer<CodeInspector, Exception> inspect,
      boolean noOuterFormals)
      throws Exception {
    R8TestCompileResult compileResult =
        testForR8(parameters.getBackend())
            .addProgramClassFileData(dumpSimple(signatures.get("Simple")))
            .addProgramClassFileData(dumpBase(signatures.get("Base")))
            .addProgramClassFileData(dumpOuter(signatures.get("Outer")))
            .addProgramClassFileData(dumpInner(signatures.get("Outer$Inner")))
            .addProgramClassFileData(dumpExtendsInner(signatures.get("Outer$ExtendsInner")))
            .addProgramClassFileData(dumpInnerInner(signatures.get("Outer$Inner$InnerInner")))
            .addProgramClassFileData(
                dumpExtendsInnerInner(signatures.get("Outer$Inner$ExtendsInnerInner")))
            .addKeepAttributes(
                ProguardKeepAttributes.INNER_CLASSES,
                ProguardKeepAttributes.ENCLOSING_METHOD,
                ProguardKeepAttributes.SIGNATURE)
            .addKeepAllClassesRuleWithAllowObfuscation()
            .allowDiagnosticMessages()
            .addOptionsModification(
                internalOptions ->
                    internalOptions.testing.disableMappingToOriginalProgramVerification = true)
            .compile();

    compileResult.assertNoErrorMessages();

    CodeInspector inspector = compileResult.inspector();

    // All classes are kept, and renamed.
    assertThat(inspector.clazz("Simple"), isPresentAndRenamed());
    assertThat(inspector.clazz("Base"), isPresentAndRenamed());
    assertThat(inspector.clazz("Outer"), isPresentAndRenamed());
    assertThat(inspector.clazz("Outer$Inner"), isPresentAndRenamed());
    assertThat(inspector.clazz("Outer$ExtendsInner"), isPresentAndRenamed());
    assertThat(inspector.clazz("Outer$Inner$InnerInner"), isPresentAndRenamed());
    assertThat(inspector.clazz("Outer$Inner$ExtendsInnerInner"), isPresentAndRenamed());

    // Test that classes with have their original signature if the default was provided.
    if (!signatures.containsKey("Simple")) {
      assertNull(inspector.clazz("Simple").getOriginalSignatureAttribute());
    }
    if (!signatures.containsKey("Base")) {
      assertEquals(baseSignature, inspector.clazz("Base").getOriginalSignatureAttribute());
    }
    if (!signatures.containsKey("Outer")) {
      assertEquals(outerSignature, inspector.clazz("Outer").getOriginalSignatureAttribute());
    }
    if (!signatures.containsKey("Outer$Inner")) {
      assertNull(inspector.clazz("Outer$Inner").getOriginalSignatureAttribute());
    }
    if (!signatures.containsKey("Outer$ExtendsInner")) {
      assertEquals(
          noOuterFormals ? extendsInnerSignatureInvalidOuter : extendsInnerSignature,
          inspector.clazz("Outer$ExtendsInner").getOriginalSignatureAttribute());
    }
    if (!signatures.containsKey("Outer$Inner$InnerInner")) {
      assertNull(inspector.clazz("Outer$Inner$InnerInner").getOriginalSignatureAttribute());
    }
    if (!signatures.containsKey("Outer$Inner$ExtendsInnerInner")) {
      assertEquals(
          noOuterFormals ? extendsInnerInnerSignatureInvalidOuter : extendsInnerInnerSignature,
          inspector.clazz("Outer$Inner$ExtendsInnerInner").getOriginalSignatureAttribute());
    }

    diagnostics.accept(compileResult.getDiagnosticMessages());
    compileResult.inspect(inspect);
  }

  private void testSingleClass(
      String name,
      String signature,
      Consumer<TestDiagnosticMessages> diagnostics,
      ThrowingConsumer<CodeInspector, Exception> inspector,
      boolean noOuterFormals)
      throws Exception {
    ImmutableMap<String, String> signatures = ImmutableMap.of(name, signature);
    runTest(signatures, diagnostics, inspector, noOuterFormals);
  }

  private void noInspection(CodeInspector inspector) {
  }

  private void noSignatureAttribute(ClassSubject clazz) {
    assertNull(clazz.getFinalSignatureAttribute());
    assertNull(clazz.getOriginalSignatureAttribute());
  }

  @Test
  public void originalJavacSignatures() throws Exception {
    // Test using the signatures generated by javac.
    runTest(ImmutableMap.of(), TestDiagnosticMessages::assertNoWarnings, this::noInspection, false);
  }

  @Test
  public void classSignature_empty() throws Exception {
    testSingleClass(
        "Outer",
        "",
        TestDiagnosticMessages::assertNoWarnings,
        inspector -> {
          ClassSubject outer = inspector.clazz("Outer");
          assertNull(outer.getFinalSignatureAttribute());
          assertNull(outer.getOriginalSignatureAttribute());
        },
        true);
  }

  @Test
  public void classSignatureExtendsInner_valid() throws Exception {
    String signature = "LOuter<TT;>.Inner;";
    testSingleClass(
        "Outer$ExtendsInner",
        signature,
        TestDiagnosticMessages::assertNoWarnings,
        inspector -> {
          ClassSubject extendsInner = inspector.clazz("Outer$ExtendsInner");
          ClassSubject outer = inspector.clazz("Outer");
          ClassSubject inner = inspector.clazz("Outer$Inner");
          String outerDescriptorWithoutSemicolon =
              outer.getFinalDescriptor().substring(0, outer.getFinalDescriptor().length() - 1);
          String innerFinalDescriptor = inner.getFinalDescriptor();
          String innerLastPart =
              innerFinalDescriptor.substring(innerFinalDescriptor.indexOf("$") + 1);
          String minifiedSignature = outerDescriptorWithoutSemicolon + "<TT;>." + innerLastPart;
          assertEquals(minifiedSignature, extendsInner.getFinalSignatureAttribute());
          assertEquals(signature, extendsInner.getOriginalSignatureAttribute());
        },
        false);
  }

  @Test
  public void classSignatureOuter_classNotFound() throws Exception {
    String signature = "<T:LNotFound;>LAlsoNotFound;";
    testSingleClass(
        "Outer",
        signature,
        TestDiagnosticMessages::assertNoWarnings,
        inspector -> {
          assertThat(inspector.clazz("NotFound"), not(isPresent()));
          ClassSubject outer = inspector.clazz("Outer");
          assertNull(outer.getOriginalSignatureAttribute());
        },
        true);
  }

  @Test
  public void classSignatureExtendsInner_innerClassNotFound() throws Exception {
    String signature = "LOuter<TT;>.NotFound;";
    testSingleClass(
        "Outer$ExtendsInner",
        signature,
        TestDiagnosticMessages::assertNoWarnings,
        inspector -> {
          assertThat(inspector.clazz("NotFound"), not(isPresent()));
          ClassSubject outer = inspector.clazz("Outer$ExtendsInner");
          // TODO(b/186745999): What to do here.
          assertNull(outer.getOriginalSignatureAttribute());
        },
        false);
  }

  @Test
  public void classSignatureExtendsInner_outerAndInnerClassNotFound() throws Exception {
    String signature = "LNotFound$AlsoNotFound;";
    testSingleClass(
        "Outer$ExtendsInner",
        signature,
        TestDiagnosticMessages::assertNoWarnings,
        inspector -> {
          assertThat(inspector.clazz("NotFound"), not(isPresent()));
          ClassSubject outer = inspector.clazz("Outer$ExtendsInner");
          assertNull(outer.getOriginalSignatureAttribute());
        },
        false);
  }

  @Test
  public void classSignatureExtendsInner_nestedInnerClassNotFound() throws Exception {
    String signature = "LOuter<TT;>.Inner.NotFound;";
    testSingleClass(
        "Outer$ExtendsInner",
        signature,
        TestDiagnosticMessages::assertNoWarnings,
        inspector -> {
          assertThat(inspector.clazz("NotFound"), not(isPresent()));
          ClassSubject outer = inspector.clazz("Outer$ExtendsInner");
          // TODO(b/1867459990): What to do here.
          assertNull(outer.getOriginalSignatureAttribute());
        },
        false);
  }

  @Test
  public void classSignatureExtendsInner_multipleNestedInnerClassesNotFound() throws Exception {
    String signature = "LOuter<TT;>.NotFound.AlsoNotFound;";
    testSingleClass(
        "Outer$ExtendsInner",
        signature,
        TestDiagnosticMessages::assertNoWarnings,
        inspector -> {
          assertThat(inspector.clazz("NotFound"), not(isPresent()));
          ClassSubject outer = inspector.clazz("Outer$ExtendsInner");
          assertNull(outer.getOriginalSignatureAttribute());
        },
        false);
  }

  @Test
  public void classSignatureOuter_invalid() throws Exception {
    testSingleClass(
        "Outer",
        "X",
        diagnostics -> {
          diagnostics.assertWarningsCount(1);
          diagnostics.assertAllWarningsMatch(
              allOf(
                  diagnosticMessage(containsString("Invalid signature 'X' for class Outer")),
                  diagnosticMessage(containsString("Expected L at position 1")),
                  diagnosticOrigin(Origin.unknown())));
        },
        inspector -> noSignatureAttribute(inspector.clazz("Outer")),
        true);
  }

  @Test
  public void classSignatureOuter_invalidEnd() throws Exception {
    testSingleClass(
        "Outer",
        "<L",
        diagnostics -> {
          diagnostics.assertWarningsCount(1);
          diagnostics.assertAllWarningsMatch(
              allOf(
                  diagnosticMessage(containsString("Invalid signature '<L' for class Outer")),
                  diagnosticMessage(containsString("Unexpected end of signature at position 3")),
                  diagnosticOrigin(Origin.unknown())));
        },
        inspector -> noSignatureAttribute(inspector.clazz("Outer")),
        true);
  }

  @Test
  public void classSignatureExtendsInner_invalid() throws Exception {
    testSingleClass(
        "Outer$ExtendsInner",
        "X",
        diagnostics -> {
          diagnostics.assertWarningsCount(1);
          diagnostics.assertAllWarningsMatch(
              allOf(
                  diagnosticMessage(
                      containsString("Invalid signature 'X' for class Outer$ExtendsInner")),
                  diagnosticMessage(containsString("Expected L at position 1")),
                  diagnosticOrigin(Origin.unknown())));
        },
        inspector -> noSignatureAttribute(inspector.clazz("Outer$ExtendsInner")),
        false);
  }

  @Test
  public void classSignatureExtendsInnerInner_invalid() throws Exception {
    testSingleClass(
        "Outer$Inner$ExtendsInnerInner",
        "X",
        diagnostics -> {
          diagnostics.assertWarningsCount(1);
          diagnostics.assertAllWarningsMatch(
              allOf(
                  diagnosticMessage(
                      containsString(
                          "Invalid signature 'X' for class Outer$Inner$ExtendsInnerInner")),
                  diagnosticMessage(containsString("Expected L at position 1")),
                  diagnosticOrigin(Origin.unknown())));
        },
        inspector -> noSignatureAttribute(inspector.clazz("Outer$Inner$ExtendsInnerInner")),
        false);
  }

  @Test
  public void multipleWarnings() throws Exception {
    runTest(
        ImmutableMap.of(
            "Outer", "X",
            "Outer$ExtendsInner", "X",
            "Outer$Inner$ExtendsInnerInner", "X"),
        diagnostics -> {
          diagnostics.assertWarningsCount(3);
        },
        inspector -> {
          noSignatureAttribute(inspector.clazz("Outer"));
          noSignatureAttribute(inspector.clazz("Outer$ExtendsInner"));
          noSignatureAttribute(inspector.clazz("Outer$Inner$ExtendsInnerInner"));
        },
        false);
  }

  @Test
  public void regress80029761() throws Exception {
    String signature = "LOuter<TT;>.com/example/Inner;";
    testSingleClass(
        "Outer$ExtendsInner",
        signature,
        diagnostics -> {
          diagnostics.assertWarningsCount(1);
          diagnostics.assertAllWarningsMatch(
              allOf(
                  diagnosticMessage(
                      containsString(
                          "Invalid signature '" + signature + "' for class Outer$ExtendsInner")),
                  diagnosticMessage(containsString("Expected ; at position 16")),
                  diagnosticOrigin(Origin.unknown())));
        },
        inspector -> {
          noSignatureAttribute(inspector.clazz("Outer$ExtendsInner"));
        },
        false);
  }
}
