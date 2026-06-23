// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import com.google.common.testing.AbstractPackageSanityTests;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.sosy_lab.common.Classes;

public class PackageSanityTest extends AbstractPackageSanityTests {

  {
    setDistinctValues(
        PersistentLinkedList.class, PersistentLinkedList.of(), PersistentLinkedList.of("test"));
    @SuppressWarnings("unchecked")
    OurSortedMap<String, String> singletonMap =
        (PathCopyingPersistentTreeMap<String, String>)
            PathCopyingPersistentTreeMap.<String, String>of().putAndCopy("test", "test");
    setDistinctValues(
        OurSortedMap.class, OurSortedMap.EmptyImmutableOurSortedMap.of(), singletonMap);
    ignoreClasses(Classes.IS_GENERATED);
  }

  {
    setDefault(SortedTreeSetUnionFind.class, new SortedTreeSetUnionFind<>());
    // ignoreClasses(Classes.IS_GENERATED);

    try {
      setDefault(Constructor.class, PackageSanityTest.class.getConstructor());
      setDefault(Method.class, PackageSanityTest.class.getDeclaredMethod("defaultMethod"));
    } catch (NoSuchMethodException e) {
      throw new AssertionError(e);
    }
  }

  @SuppressWarnings("unused")
  private static void defaultMethod() {}
}
