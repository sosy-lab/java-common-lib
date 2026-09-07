// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find.benchmarking;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Var;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.union_find.AbstractImmutableParentPointerTreeBuilder;
import org.sosy_lab.common.collect.union_find.AbstractImmutableUnionFind;
import org.sosy_lab.common.collect.union_find.ImmutableParentPointerTreeUnionFind;
import org.sosy_lab.common.collect.union_find.ImmutableSortedParentPointerTreeUnionFind;
import org.sosy_lab.common.collect.union_find.ParentPointerTreeUnionFind;
import org.sosy_lab.common.collect.union_find.ParentPointerTreeUnionFind.UnionType;
import org.sosy_lab.common.collect.union_find.PersistentParentPointerTreeUnionFind;
import org.sosy_lab.common.collect.union_find.PersistentSortedParentPointerTreeUnionFind;
import org.sosy_lab.common.collect.union_find.PersistentUnionFind;
import org.sosy_lab.common.collect.union_find.SortedParentPointerTreeUnionFind;
import org.sosy_lab.common.collect.union_find.UnionFind;

public final class Algs4DatasetBenchmark {

  static final Pattern PATTERN = Pattern.compile("\\s+");

  public static void main(String[] args) {

    @Var boolean immutable = false;
    @Var boolean persistent = false;
    @Var boolean sorted = false;
    @Var boolean unionByRank = false;
    @Var
    @Nullable Path filePath = null;
    List<Integer> unionInput = new ArrayList<>();

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
          filePath = Path.of(string);
        }
      }
    }

    try {
      Preconditions.checkNotNull(filePath);

      for (String line : Files.readAllLines(filePath)) {

        String trimmedLine = line.trim();

        if (trimmedLine.isEmpty()) {
          continue;
        }

        List<String> tokens = Splitter.on(PATTERN).splitToList(trimmedLine);

        unionInput.add(Integer.parseInt(tokens.get(0)));
        unionInput.add(Integer.parseInt(tokens.get(1)));
      }
    } catch (IOException e) {
      System.exit(1);
    }

    Iterator<Integer> iterator = unionInput.iterator();

    if (!immutable && !persistent && !sorted) {
      if (unionByRank) {
        mutable(new ParentPointerTreeUnionFind<>(UnionType.UNION_BY_RANK), iterator);
      } else {
        mutable(new ParentPointerTreeUnionFind<>(UnionType.UNION_BY_SIZE), iterator);
      }
    } else if (!immutable && !persistent && sorted) {
      if (unionByRank) {
        mutable(new SortedParentPointerTreeUnionFind<>(UnionType.UNION_BY_RANK), iterator);
      } else {
        mutable(new SortedParentPointerTreeUnionFind<>(UnionType.UNION_BY_SIZE), iterator);
      }
    } else if (immutable && !sorted) {
      if (unionByRank) {
        immutable(
            ImmutableParentPointerTreeUnionFind.Builder.getBuilder(UnionType.UNION_BY_RANK),
            iterator);
      } else {
        immutable(
            ImmutableParentPointerTreeUnionFind.Builder.getBuilder(UnionType.UNION_BY_SIZE),
            iterator);
      }
    } else if (immutable && sorted) {
      if (unionByRank) {
        immutable(
            ImmutableSortedParentPointerTreeUnionFind.Builder.getBuilder(UnionType.UNION_BY_RANK),
            iterator);
      } else {
        immutable(
            ImmutableSortedParentPointerTreeUnionFind.Builder.getBuilder(UnionType.UNION_BY_SIZE),
            iterator);
      }
    } else if (persistent && !sorted) {
      if (unionByRank) {
        persistent(PersistentParentPointerTreeUnionFind.of(UnionType.UNION_BY_RANK), iterator);
      } else {
        persistent(PersistentParentPointerTreeUnionFind.of(UnionType.UNION_BY_SIZE), iterator);
      }
    } else if (persistent && sorted) {
      if (unionByRank) {
        persistent(
            PersistentSortedParentPointerTreeUnionFind.of(UnionType.UNION_BY_RANK), iterator);
      } else {
        persistent(
            PersistentSortedParentPointerTreeUnionFind.of(UnionType.UNION_BY_SIZE), iterator);
      }
    } else {
      System.exit(1);
    }
    System.exit(0);
  }

  private static void mutable(UnionFind<Integer> pUnionFind, Iterator<Integer> pIterator) {

    while (pIterator.hasNext()) {

      int a = pIterator.next();
      int b = pIterator.next();

      pUnionFind.union(a, b);
    }
  }

  @CanIgnoreReturnValue
  private static AbstractImmutableUnionFind<Integer> immutable(
      AbstractImmutableParentPointerTreeBuilder<Integer> pBuilder, Iterator<Integer> pIterator) {

    while (pIterator.hasNext()) {

      int a = pIterator.next();
      int b = pIterator.next();

      pBuilder.union(a, b);
    }

    return pBuilder.build();
  }

  private static void persistent(
      PersistentUnionFind<Integer> pUnionFind, Iterator<Integer> pIterator) {

    @Var PersistentUnionFind<Integer> unionFind = pUnionFind;

    while (pIterator.hasNext()) {

      int a = pIterator.next();
      int b = pIterator.next();

      unionFind = unionFind.unionAndCopy(a, b);
    }
  }

  private Algs4DatasetBenchmark() {}
}
