// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

/**
 * An implementation of {@link AbstractTreeNode} resulting in nodes that can be used as non-root
 * nodes or root nodes. Their primary intended use is as root nodes. Rank describes the maximum
 * height of the tree of this node and its child nodes (i.e. without path compression). Size
 * describes the total number of elements in the tree represented by this root node.
 *
 * @param <T> type of elements each node holds as value
 */
public final class RootNode<T> extends AbstractTreeNode<T> {

  private int rank;
  private int size;

  /**
   * Constructor for a root node. The parent variable points to itself, thus indicating this is a
   * root node. If appended to another tree, parent can be reallocated to the new parent node, while
   * the current node simply functions as a non-root node from then on. In the beginning, rank is 0
   * and size is 1.
   *
   * @param pValue element to be stored in the node
   */
  public RootNode(T pValue) {

    super(pValue);

    this.rank = 0;
    this.size = 1;
  }

  public int getRank() {
    return rank;
  }

  public int getSize() {
    return size;
  }

  /** Increments rank by one. */
  public void incrementRankByOne() {
    rank++;
  }

  /** Increments size by one. */
  public void incrementSizeByOne() {
    size++;
  }

  /**
   * Increments size by pN.
   *
   * @param pN number by which size is to be increased.
   */
  public void incrementSizeBy(int pN) {
    size += pN;
  }
}
