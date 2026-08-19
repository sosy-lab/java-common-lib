// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

import com.google.errorprone.annotations.Immutable;

/**
 * An implementation of {@link AbstractImmutableTreeNode} resulting in immutable nodes that can be
 * used as non-root nodes or root nodes. Their primary intended use is as root nodes. Rank describes
 * the maximum height of the tree of this node and its child nodes (i.e. without path compression).
 * Size describes the total number of elements in the tree represented by this root node.
 *
 * @param <T> type of elements each node holds as value
 */
@Immutable(containerOf = "T")
public class ImmutableRootNode<T> extends AbstractImmutableTreeNode<T> {

  private final int rank;
  private final int size;

  protected ImmutableRootNode(T pValue, Integer pRank, Integer pSize) {

    super(pValue);

    rank = pRank;
    size = pSize;
  }

  public int getRank() {
    return rank;
  }

  public int getSize() {
    return size;
  }
}
