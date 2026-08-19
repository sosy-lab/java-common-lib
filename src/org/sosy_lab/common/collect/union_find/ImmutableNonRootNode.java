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
 * An implementation of {@link AbstractImmutableTreeNode} resulting in immutable nodes that can only
 * be used as non-root nodes but not as root nodes.
 *
 * @param <T> type of elements each node holds as value
 */
@Immutable(containerOf = "T")
public final class ImmutableNonRootNode<T> extends AbstractImmutableTreeNode<T> {

  public ImmutableNonRootNode(AbstractImmutableTreeNode<T> pParent, T pValue) {
    super(pParent, pValue);
  }
}
