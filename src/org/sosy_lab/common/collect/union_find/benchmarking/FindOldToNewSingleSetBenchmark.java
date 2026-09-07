// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find.benchmarking;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Var;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.common.collect.union_find.AbstractImmutableParentPointerTreeBuilder;
import org.sosy_lab.common.collect.union_find.ImmutableParentPointerTreeUnionFind;
import org.sosy_lab.common.collect.union_find.ImmutableSortedParentPointerTreeUnionFind;
import org.sosy_lab.common.collect.union_find.ParentPointerTreeUnionFind;
import org.sosy_lab.common.collect.union_find.ParentPointerTreeUnionFind.UnionType;
import org.sosy_lab.common.collect.union_find.PersistentParentPointerTreeUnionFind;
import org.sosy_lab.common.collect.union_find.PersistentSortedParentPointerTreeUnionFind;
import org.sosy_lab.common.collect.union_find.PersistentUnionFind;
import org.sosy_lab.common.collect.union_find.SortedParentPointerTreeUnionFind;
import org.sosy_lab.common.collect.union_find.UnionFind;

public class FindOldToNewSingleSetBenchmark {

  static final Pattern PATTERN = Pattern.compile("\\s+");

  public static void main(String[] args) {

    @Var boolean immutable = false;
    @Var boolean persistent = false;
    @Var boolean sorted = false;
    @Var boolean unionByRank = false;
    @Var int n = 0;

    if (args.length == 0) {
      System.exit(1);
    }

    for (String string : args) {

      switch (string) {
        case "-immutable" -> {
          immutable = true;
        }

        case "-persistent" -> {
          persistent = true;
        }

        case "-sorted" -> sorted = true;

        case "-rank" -> unionByRank = true;

        default -> {
          Matcher matcher = PATTERN.matcher(string);

          if (matcher.find()) {
            n = Integer.parseInt(matcher.group(1));
          } else {
            throw new IllegalArgumentException("Incompatible args");
          }
        }
      }
    }

    if (!immutable && !persistent && !sorted) {
      if (unionByRank) {
        mutable(new ParentPointerTreeUnionFind<>(UnionType.UNION_BY_RANK), n);
      } else {
        mutable(new ParentPointerTreeUnionFind<>(UnionType.UNION_BY_SIZE), n);
      }
    } else if (!immutable && !persistent && sorted) {
      if (unionByRank) {
        mutable(new SortedParentPointerTreeUnionFind<>(UnionType.UNION_BY_RANK), n);
      } else {
        mutable(new SortedParentPointerTreeUnionFind<>(UnionType.UNION_BY_SIZE), n);
      }
    } else if (immutable && !sorted) {
      if (unionByRank) {
        immutable(
            ImmutableParentPointerTreeUnionFind.Builder.getBuilder(UnionType.UNION_BY_RANK), n);
      } else {
        immutable(
            ImmutableParentPointerTreeUnionFind.Builder.getBuilder(UnionType.UNION_BY_SIZE), n);
      }
    } else if (immutable && sorted) {
      if (unionByRank) {
        immutable(
            ImmutableSortedParentPointerTreeUnionFind.Builder.getBuilder(UnionType.UNION_BY_RANK),
            n);
      } else {
        immutable(
            ImmutableSortedParentPointerTreeUnionFind.Builder.getBuilder(UnionType.UNION_BY_SIZE),
            n);
      }
    } else if (persistent && !sorted) {
      if (unionByRank) {
        persistent(PersistentParentPointerTreeUnionFind.of(UnionType.UNION_BY_RANK), n);
      } else {
        persistent(PersistentParentPointerTreeUnionFind.of(UnionType.UNION_BY_SIZE), n);
      }
    } else if (persistent && sorted) {
      if (unionByRank) {
        persistent(PersistentSortedParentPointerTreeUnionFind.of(UnionType.UNION_BY_RANK), n);
      } else {
        persistent(PersistentSortedParentPointerTreeUnionFind.of(UnionType.UNION_BY_SIZE), n);
      }
    } else {
      System.exit(1);
    }

    System.exit(0);
  }

  private static void mutable(UnionFind<Integer> pUnionFind, int pN) {

    for (int i = 0; i < pN; i++) {
      pUnionFind.union(0, i);
    }

    performFinds(pUnionFind, pN);
  }

  private static void immutable(
      AbstractImmutableParentPointerTreeBuilder<Integer> pBuilder, int pN) {

    for (int i = 0; i < pN; i++) {
      pBuilder.union(0, i);
    }

    performFinds(pBuilder.build(), pN);
  }

  private static void persistent(PersistentUnionFind<Integer> pUnionFind, int pN) {

    @Var PersistentUnionFind<Integer> unionFind = pUnionFind;

    for (int i = 0; i < pN; i++) {
      unionFind = unionFind.unionAndCopy(0, i);
    }

    performFinds(unionFind, pN);
  }

  @CanIgnoreReturnValue
  private static int performFinds(UnionFind<Integer> pUnionFind, int pN) {

    @Var int root = 0;

    for (int i = 0; i < pN; i++) {

      root = pUnionFind.find(i);
    }

    return root;
  }

  private FindOldToNewSingleSetBenchmark() {}
}
