/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.common;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.testing.ClassSanityTester;
import java.util.Comparator;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.junit.Ignore;
import org.junit.Test;

/** Tests for {@link OptionalComparators}. */
public final class OptionalComparatorsTest {

  @Test
  @Ignore("Guava's comparators do not have equals()")
  public void testEquals() throws Exception {
    new ClassSanityTester()
        .forAllPublicStaticMethods(Optionals.class)
        .thatReturn(Comparator.class)
        .testEquals();
  }

  @Test
  public void testNulls() throws Exception {
    new ClassSanityTester()
        .setDefault(OptionalInt.class, OptionalInt.empty())
        .setDefault(OptionalLong.class, OptionalLong.empty())
        .setDefault(OptionalDouble.class, OptionalDouble.empty())
        .forAllPublicStaticMethods(Optionals.class)
        .thatReturn(Comparator.class)
        .testNulls();
  }

  @Test
  @Ignore("Guava's comparators are not serializable")
  public void testSerializable() throws Exception {
    new ClassSanityTester()
        .forAllPublicStaticMethods(Optionals.class)
        .thatReturn(Comparator.class)
        .testSerializable();
  }

  private static final OptionalInt I1 = OptionalInt.of(-1);
  private static final OptionalInt I2 = OptionalInt.of(1);
  private static final OptionalInt I_EMPTY = OptionalInt.empty();

  @Test
  public void testComparingIntEmptyFirst_AllPresent() {
    Comparator<OptionalInt> comp = Optionals.comparingIntEmptyFirst();
    assertThat(comp.compare(I1, I1)).isEqualTo(0);
    assertThat(comp.compare(I2, I2)).isEqualTo(0);
    assertThat(comp.compare(I1, I2)).isLessThan(0);
    assertThat(comp.compare(I2, I1)).isGreaterThan(0);
  }

  @Test
  public void testComparingIntEmptyFirst_IomePresent() {
    Comparator<OptionalInt> comp = Optionals.comparingIntEmptyFirst();
    assertThat(comp.compare(I1, I_EMPTY)).isGreaterThan(0);
    assertThat(comp.compare(I2, I_EMPTY)).isGreaterThan(0);
    assertThat(comp.compare(I_EMPTY, I1)).isLessThan(0);
    assertThat(comp.compare(I_EMPTY, I2)).isLessThan(0);
  }

  @Test
  public void testComparingIntEmptyFirst_NonPresent() {
    Comparator<OptionalInt> comp = Optionals.comparingIntEmptyFirst();
    assertThat(comp.compare(I_EMPTY, I_EMPTY)).isEqualTo(0);
  }

  @Test
  public void testComparingIntEmptyLast_AllPresent() {
    Comparator<OptionalInt> comp = Optionals.comparingIntEmptyLast();
    assertThat(comp.compare(I1, I1)).isEqualTo(0);
    assertThat(comp.compare(I2, I2)).isEqualTo(0);
    assertThat(comp.compare(I1, I2)).isLessThan(0);
    assertThat(comp.compare(I2, I1)).isGreaterThan(0);
  }

  @Test
  public void testComparingIntEmptyLast_IomePresent() {
    Comparator<OptionalInt> comp = Optionals.comparingIntEmptyLast();
    assertThat(comp.compare(I1, I_EMPTY)).isLessThan(0);
    assertThat(comp.compare(I2, I_EMPTY)).isLessThan(0);
    assertThat(comp.compare(I_EMPTY, I1)).isGreaterThan(0);
    assertThat(comp.compare(I_EMPTY, I2)).isGreaterThan(0);
  }

  @Test
  public void testComparingIntEmptyLast_NonPresent() {
    Comparator<OptionalInt> comp = Optionals.comparingIntEmptyLast();
    assertThat(comp.compare(I_EMPTY, I_EMPTY)).isEqualTo(0);
  }

  private static final OptionalLong L1 = OptionalLong.of(-1);
  private static final OptionalLong L2 = OptionalLong.of(1);
  private static final OptionalLong L_EMPTY = OptionalLong.empty();

  @Test
  public void testComparingLongEmptyFirst_AllPresent() {
    Comparator<OptionalLong> comp = Optionals.comparingLongEmptyFirst();
    assertThat(comp.compare(L1, L1)).isEqualTo(0);
    assertThat(comp.compare(L2, L2)).isEqualTo(0);
    assertThat(comp.compare(L1, L2)).isLessThan(0);
    assertThat(comp.compare(L2, L1)).isGreaterThan(0);
  }

  @Test
  public void testComparingLongEmptyFirst_LomePresent() {
    Comparator<OptionalLong> comp = Optionals.comparingLongEmptyFirst();
    assertThat(comp.compare(L1, L_EMPTY)).isGreaterThan(0);
    assertThat(comp.compare(L2, L_EMPTY)).isGreaterThan(0);
    assertThat(comp.compare(L_EMPTY, L1)).isLessThan(0);
    assertThat(comp.compare(L_EMPTY, L2)).isLessThan(0);
  }

  @Test
  public void testComparingLongEmptyFirst_NonPresent() {
    Comparator<OptionalLong> comp = Optionals.comparingLongEmptyFirst();
    assertThat(comp.compare(L_EMPTY, L_EMPTY)).isEqualTo(0);
  }

  @Test
  public void testComparingLongEmptyLast_AllPresent() {
    Comparator<OptionalLong> comp = Optionals.comparingLongEmptyLast();
    assertThat(comp.compare(L1, L1)).isEqualTo(0);
    assertThat(comp.compare(L2, L2)).isEqualTo(0);
    assertThat(comp.compare(L1, L2)).isLessThan(0);
    assertThat(comp.compare(L2, L1)).isGreaterThan(0);
  }

  @Test
  public void testComparingLongEmptyLast_LomePresent() {
    Comparator<OptionalLong> comp = Optionals.comparingLongEmptyLast();
    assertThat(comp.compare(L1, L_EMPTY)).isLessThan(0);
    assertThat(comp.compare(L2, L_EMPTY)).isLessThan(0);
    assertThat(comp.compare(L_EMPTY, L1)).isGreaterThan(0);
    assertThat(comp.compare(L_EMPTY, L2)).isGreaterThan(0);
  }

  @Test
  public void testComparingLongEmptyLast_NonPresent() {
    Comparator<OptionalLong> comp = Optionals.comparingLongEmptyLast();
    assertThat(comp.compare(L_EMPTY, L_EMPTY)).isEqualTo(0);
  }

  private static final OptionalDouble D1 = OptionalDouble.of(-1);
  private static final OptionalDouble D2 = OptionalDouble.of(1);
  private static final OptionalDouble D_EMPTY = OptionalDouble.empty();

  @Test
  public void testComparingDoubleEmptyFirst_AllPresent() {
    Comparator<OptionalDouble> comp = Optionals.comparingDoubleEmptyFirst();
    assertThat(comp.compare(D1, D1)).isEqualTo(0);
    assertThat(comp.compare(D2, D2)).isEqualTo(0);
    assertThat(comp.compare(D1, D2)).isLessThan(0);
    assertThat(comp.compare(D2, D1)).isGreaterThan(0);
  }

  @Test
  public void testComparingDoubleEmptyFirst_DomePresent() {
    Comparator<OptionalDouble> comp = Optionals.comparingDoubleEmptyFirst();
    assertThat(comp.compare(D1, D_EMPTY)).isGreaterThan(0);
    assertThat(comp.compare(D2, D_EMPTY)).isGreaterThan(0);
    assertThat(comp.compare(D_EMPTY, D1)).isLessThan(0);
    assertThat(comp.compare(D_EMPTY, D2)).isLessThan(0);
  }

  @Test
  public void testComparingDoubleEmptyFirst_NonPresent() {
    Comparator<OptionalDouble> comp = Optionals.comparingDoubleEmptyFirst();
    assertThat(comp.compare(D_EMPTY, D_EMPTY)).isEqualTo(0);
  }

  @Test
  public void testComparingDoubleEmptyDast_AllPresent() {
    Comparator<OptionalDouble> comp = Optionals.comparingDoubleEmptyLast();
    assertThat(comp.compare(D1, D1)).isEqualTo(0);
    assertThat(comp.compare(D2, D2)).isEqualTo(0);
    assertThat(comp.compare(D1, D2)).isLessThan(0);
    assertThat(comp.compare(D2, D1)).isGreaterThan(0);
  }

  @Test
  public void testComparingDoubleEmptyDast_DomePresent() {
    Comparator<OptionalDouble> comp = Optionals.comparingDoubleEmptyLast();
    assertThat(comp.compare(D1, D_EMPTY)).isLessThan(0);
    assertThat(comp.compare(D2, D_EMPTY)).isLessThan(0);
    assertThat(comp.compare(D_EMPTY, D1)).isGreaterThan(0);
    assertThat(comp.compare(D_EMPTY, D2)).isGreaterThan(0);
  }

  @Test
  public void testComparingDoubleEmptyDast_NonPresent() {
    Comparator<OptionalDouble> comp = Optionals.comparingDoubleEmptyLast();
    assertThat(comp.compare(D_EMPTY, D_EMPTY)).isEqualTo(0);
  }
}
