// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

import javax.annotation.Nullable;

public class TreeNode<T> {

  @Nullable TreeNode<T> parent;
  T value;

  private TreeNode(T value) {
    this.parent = null;
    this.value = value;
  }

  private TreeNode(TreeNode<T> parent, T value) {
    this.parent = parent;
    this.value = value;
  }

  @Nullable
  public TreeNode<T> getParent() {
    return parent;
  }

  public T getValue() {
    return value;
  }

  public static <V> TreeNode<V> getNewRootNode(V value) {
    return new TreeNode<>(value);
  }

  public static <V> TreeNode<V> getNewNode(TreeNode<V> parent, V value) {
    return new TreeNode<>(parent, value);
  }
}
