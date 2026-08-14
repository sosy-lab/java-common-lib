// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

public final class RootNode<T> extends AbstractTreeNode<T> {

  private int rank;
  private int size;

  public RootNode(T value) {

    super(value);

    this.rank = 0;
    this.size = 1;
  }

  public int getRank() {
    return rank;
  }

  public int getSize() {
    return size;
  }

  public void incrementRankByOne() {
    rank++;
  }

  public void incrementSizeByOne() {
    size++;
  }

  public void incrementSizeBy(int n) {
    size += n;
  }
}
