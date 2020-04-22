// Copyright (c) 2020, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.kotlin.metadata;

import static com.android.tools.r8.utils.codeinspector.Matchers.isPresent;
import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

import com.android.tools.r8.TestParameters;
import com.android.tools.r8.ToolHelper;
import com.android.tools.r8.ToolHelper.KotlinTargetVersion;
import com.android.tools.r8.shaking.ProguardKeepAttributes;
import com.android.tools.r8.utils.codeinspector.ClassSubject;
import com.android.tools.r8.utils.codeinspector.CodeInspector;
import com.android.tools.r8.utils.codeinspector.FoundClassSubject;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import kotlinx.metadata.jvm.KotlinClassMetadata;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class MetadataRewritePassThroughTest extends KotlinMetadataTestBase {

  @Parameterized.Parameters(name = "{0} target: {1}")
  public static Collection<Object[]> data() {
    return buildParameters(
        getTestParameters().withCfRuntimes().build(), KotlinTargetVersion.values());
  }

  private final TestParameters parameters;

  public MetadataRewritePassThroughTest(
      TestParameters parameters, KotlinTargetVersion targetVersion) {
    super(targetVersion);
    this.parameters = parameters;
  }

  @Test
  public void testKotlinStdLib() throws Exception {
    testForR8(parameters.getBackend())
        .addProgramFiles(ToolHelper.getKotlinStdlibJar())
        .setMinApi(parameters.getApiLevel())
        .addKeepAllClassesRule()
        .addKeepRules("-keep class kotlin.Metadata")
        .addKeepAttributes(ProguardKeepAttributes.RUNTIME_VISIBLE_ANNOTATIONS)
        .compile()
        .inspect(this::inspect);
  }

  public void inspect(CodeInspector inspector) throws IOException, ExecutionException {
    CodeInspector stdLibInspector = new CodeInspector(ToolHelper.getKotlinStdlibJar());
    for (FoundClassSubject clazzSubject : stdLibInspector.allClasses()) {
      ClassSubject r8Clazz = inspector.clazz(clazzSubject.getOriginalName());
      assertThat(r8Clazz, isPresent());
      KotlinClassMetadata originalMetadata = clazzSubject.getKotlinClassMetadata();
      if (originalMetadata == null) {
        assertNull(r8Clazz.getKotlinClassMetadata());
        continue;
      }
      assertNotNull(r8Clazz.getKotlinClassMetadata());
      // TODO(b/152153136): Extend the test with assertions about metadata equality.
    }
  }
}