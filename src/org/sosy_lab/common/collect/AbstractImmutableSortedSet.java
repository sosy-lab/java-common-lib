// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import com.google.errorprone.annotations.Immutable;
import java.util.NavigableSet;

@Immutable(containerOf = {"E"})
public abstract class AbstractImmutableSortedSet<E extends Comparable<? super E>>
    extends AbstractImmutableSet<E> implements NavigableSet<E> {

  // TODO: do we want/need to implement something here?
}
