// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

public final class NonRootNode<T> extends AbstractTreeNode<T> {

  public NonRootNode(AbstractTreeNode<T> parent, T value) {
    super(parent, value);
  }
}
