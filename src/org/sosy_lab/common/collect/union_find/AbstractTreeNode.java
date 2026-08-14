// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

/**
 * An abstract class of nodes from which a simple parent pointer tree can be built.
 *
 * @param <T> type of elements each node holds as value
 */
public abstract class AbstractTreeNode<T> {

  private AbstractTreeNode<T> parent;
  private final T value;

  /**
   * Constructor for a root node. The parent variable points to itself, thus indicating this is a
   * root node. If appended to another tree, parent can be reallocated to the new parent node, while
   * the current node simply functions as a non-root node from then on.
   *
   * @param value element to be stored in the node
   */
  protected AbstractTreeNode(T value) {
    this.parent = this;
    this.value = value;
  }

  /**
   * Constructor for a non-root node.
   *
   * @param parent parent node (can be root or non-root)
   * @param value element to be stored in the node
   */
  protected AbstractTreeNode(AbstractTreeNode<T> parent, T value) {
    this.parent = parent;
    this.value = value;
  }

  public AbstractTreeNode<T> getParent() {
    return parent;
  }

  public void setParent(AbstractTreeNode<T> parent) {
    this.parent = parent;
  }

  public T getValue() {
    return value;
  }
}
