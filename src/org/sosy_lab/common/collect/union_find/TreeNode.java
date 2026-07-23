// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

public class TreeNode<T> {

  TreeNode<T> parent;
  T value;

  public TreeNode(TreeNode<T> parent, T value) {
    this.parent = parent;
    this.value = value;
  }

  public TreeNode<T> getParent() {
    return parent;
  }

  public T getValue() {
    return value;
  }
}
