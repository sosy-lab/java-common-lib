// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

/**
 * An implementation of {@link AbstractTreeNode} resulting in nodes that can only be used as
 * non-root nodes but not as root nodes.
 *
 * @param <T> type of elements each node holds as value
 */
public final class NonRootNode<T> extends AbstractTreeNode<T> {

  /**
   * Constructor for a non-root node.
   *
   * @param pParent parent node (can be root or non-root)
   * @param pValue element to be stored in the node
   */
  public NonRootNode(AbstractTreeNode<T> pParent, T pValue) {
    super(pParent, pValue);
  }
}
