// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

public abstract class AbstractTreeNode<T> {

  private AbstractTreeNode<T> parent;
  private final T value;

  protected AbstractTreeNode(T value) {
    this.parent = this;
    this.value = value;
  }

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
