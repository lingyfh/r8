// Copyright (c) 2019, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.naming.signature;

import static com.google.common.base.Predicates.alwaysFalse;

import com.android.tools.r8.graph.AppView;
import com.android.tools.r8.graph.DexProgramClass;
import com.android.tools.r8.graph.DexType;
import com.android.tools.r8.graph.GenericSignatureContextBuilder;
import com.android.tools.r8.graph.GenericSignaturePartialTypeArgumentApplier;
import com.android.tools.r8.graph.GenericSignatureTypeRewriter;
import com.android.tools.r8.naming.NamingLens;
import com.android.tools.r8.utils.IterableUtils;
import com.android.tools.r8.utils.ThreadUtils;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

// TODO(b/169516860): We should generalize this to handle rewriting of attributes in general.
public class GenericSignatureRewriter {

  private final AppView<?> appView;
  private final NamingLens namingLens;
  private final GenericSignatureContextBuilder contextBuilder;

  public GenericSignatureRewriter(AppView<?> appView, NamingLens namingLens) {
    this(appView, namingLens, null);
  }

  public GenericSignatureRewriter(
      AppView<?> appView, NamingLens namingLens, GenericSignatureContextBuilder contextBuilder) {
    this.appView = appView;
    this.namingLens = namingLens;
    this.contextBuilder = contextBuilder;
  }

  public void run(Iterable<? extends DexProgramClass> classes, ExecutorService executorService)
      throws ExecutionException {
    // Rewrite signature annotations for applications that are minified or if we have liveness
    // information, since we could have pruned types.
    if (namingLens.isIdentityLens()
        && !appView.appInfo().hasLiveness()
        && !appView.options().parseSignatureAttribute()) {
      return;
    }
    // Classes may not be the same as appInfo().classes() if applymapping is used on classpath
    // arguments. If that is the case, the ProguardMapMinifier will pass in all classes that is
    // either ProgramClass or has a mapping. This is then transitively called inside the
    // ClassNameMinifier.
    Predicate<DexType> wasPruned =
        appView.hasLiveness() ? appView.withLiveness().appInfo()::wasPruned : alwaysFalse();
    Predicate<DexType> hasGenericTypeVariables =
        type -> GenericSignatureContextBuilder.hasGenericTypeVariables(appView, type, wasPruned);
    BiPredicate<DexType, DexType> hasPrunedRelationship =
        (enclosing, enclosed) ->
            contextBuilder.hasPrunedRelationship(appView, enclosing, enclosed, wasPruned);
    ThreadUtils.processItems(
        // Final merging of classes can introduce pruned types that still exists in classes, we
        // therefore prune them from work here.
        IterableUtils.filter(classes, clazz -> !wasPruned.test(clazz.getType())),
        clazz -> {
          GenericSignaturePartialTypeArgumentApplier classArgumentApplier =
              contextBuilder != null
                  ? GenericSignaturePartialTypeArgumentApplier.build(
                      appView,
                      contextBuilder.computeTypeParameterContext(
                          appView, clazz.getType(), wasPruned),
                      hasPrunedRelationship,
                      hasGenericTypeVariables)
                  : null;
          GenericSignatureTypeRewriter genericSignatureTypeRewriter =
              new GenericSignatureTypeRewriter(appView, clazz, hasGenericTypeVariables);
          clazz.setClassSignature(
              genericSignatureTypeRewriter.rewrite(
                  classArgumentApplier != null
                      ? classArgumentApplier.visitClassSignature(clazz.getClassSignature())
                      : clazz.getClassSignature()));
          clazz.forEachField(
              field ->
                  field.setGenericSignature(
                      genericSignatureTypeRewriter.rewrite(
                          classArgumentApplier != null
                              ? classArgumentApplier.visitFieldTypeSignature(
                                  field.getGenericSignature())
                              : field.getGenericSignature())));
          clazz.forEachMethod(
              method -> {
                // The reflection api do not distinguish static methods context and
                // from virtual methods we therefore always base the context for a method on
                // the class context.
                method.setGenericSignature(
                    genericSignatureTypeRewriter.rewrite(
                        classArgumentApplier != null
                            ? classArgumentApplier
                                .buildForMethod(
                                    method.getGenericSignature().getFormalTypeParameters())
                                .visitMethodSignature(method.getGenericSignature())
                            : method.getGenericSignature()));
              });
        },
        executorService);
  }
}
