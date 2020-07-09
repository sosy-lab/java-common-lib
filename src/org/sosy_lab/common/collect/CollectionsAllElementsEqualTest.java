// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CollectionsAllElementsEqualTest {

  @Parameters(name = "{0}: {1}")
  public static List<Object[]> parameters() {
    return ImmutableList.of(
        new Object[] {Lists.newArrayList(""), true},
        new Object[] {Lists.newArrayList("", ""), true},
        new Object[] {Lists.newArrayList("", "a"), false},
        new Object[] {Lists.newArrayList("a", "a", "a"), true},
        new Object[] {Lists.newArrayList("a", "a", ""), false},
        new Object[] {Lists.newArrayList((String) null), true},
        new Object[] {Lists.newArrayList(null, null), true},
        new Object[] {Lists.newArrayList("", null), false},
        new Object[] {Lists.newArrayList(null, ""), false},
        new Object[] {Lists.newArrayList(null, null, null), true});
  }

  @Parameter(0)
  public List<String> inputs;

  @Parameter(1)
  public boolean expectedResult;

  @Test
  public void testArray() {
    assertThat(Collections3.allElementsEqual(inputs.toArray())).isEqualTo(expectedResult);
  }

  @Test
  public void testList() {
    assertThat(Collections3.allElementsEqual(new ArrayList<>(inputs))).isEqualTo(expectedResult);
  }

  @Test
  public void testStream() {
    assertThat(Collections3.allElementsEqual(inputs.stream())).isEqualTo(expectedResult);
  }
}
