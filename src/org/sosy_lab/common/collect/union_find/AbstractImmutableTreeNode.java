// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

import com.google.errorprone.annotations.DoNotCall;
import com.google.errorprone.annotations.Immutable;

/**
 * An abstract class of immutable nodes from which a simple immutable parent pointer tree can be
 * built.
 *
 * @param <T> type of elements each node holds as value
 */
@Immutable(containerOf = "T")
public class AbstractImmutableTreeNode<T> extends AbstractTreeNode<T> {

  protected AbstractImmutableTreeNode(T pValue) {
    super(pValue);
  }

  protected AbstractImmutableTreeNode(AbstractTreeNode<T> pParent, T pValue) {
    super(pParent, pValue);
  }

  /**
   * @throws UnsupportedOperationException Always.
   * @deprecated Unsupported operation.
   */
  @Deprecated
  @Override
  @DoNotCall
  public void setParent(AbstractTreeNode<T> pParent) {
    throw new UnsupportedOperationException();
  }
}
