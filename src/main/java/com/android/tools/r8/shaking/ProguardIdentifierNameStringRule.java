// Copyright (c) 2017, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package com.android.tools.r8.shaking;

import com.android.tools.r8.graph.DexAccessFlags;
import java.util.Set;

public class ProguardIdentifierNameStringRule extends ProguardConfigurationRule {

  public static class Builder extends ProguardConfigurationRule.Builder {
    private Builder() {
    }

    public ProguardIdentifierNameStringRule build() {
      return new ProguardIdentifierNameStringRule(classAnnotation, classAccessFlags,
          negatedClassAccessFlags, classTypeNegated, classType, classNames, inheritanceAnnotation,
          inheritanceClassName, inheritanceIsExtends, memberRules);
    }
  }

  private ProguardIdentifierNameStringRule(
      ProguardTypeMatcher classAnnotation,
      DexAccessFlags classAccessFlags,
      DexAccessFlags negatedClassAccessFlags,
      boolean classTypeNegated,
      ProguardClassType classType,
      ProguardClassNameList classNames,
      ProguardTypeMatcher inheritanceAnnotation,
      ProguardTypeMatcher inheritanceClassName,
      boolean inheritanceIsExtends,
      Set<ProguardMemberRule> memberRules) {
    super(classAnnotation, classAccessFlags, negatedClassAccessFlags, classTypeNegated, classType,
        classNames, inheritanceAnnotation, inheritanceClassName, inheritanceIsExtends, memberRules);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  String typeString() {
    return "identifiernamestring";
  }

  public static ProguardIdentifierNameStringRule defaultAllRule() {
    ProguardIdentifierNameStringRule.Builder builder = ProguardIdentifierNameStringRule.builder();
    builder.setClassNames(
        ProguardClassNameList.singletonList(ProguardTypeMatcher.defaultAllMatcher()));
    return builder.build();
  }

}
