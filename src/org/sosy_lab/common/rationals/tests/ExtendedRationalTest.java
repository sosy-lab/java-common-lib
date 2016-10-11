package org.sosy_lab.common.rationals.tests;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.common.rationals.ExtendedRational;
import org.sosy_lab.common.rationals.Rational;

public class ExtendedRationalTest {
  @Test
  public void testIsRationalNumber() {
    ExtendedRational x = new ExtendedRational(Rational.ofLongs(23, 7));
    assertThat(x.isRational()).isTrue();
  }

  @Test
  public void testIsRationalInfty() {
    ExtendedRational x = ExtendedRational.INFTY;
    assertThat(x.isRational()).isFalse();
  }

  @Test
  public void testInstantiation() {
    ExtendedRational x;
    x = new ExtendedRational(Rational.ofLongs(108, 96));
    Assert.assertEquals("9/8", x.toString());
    assertThat(x.toString()).isEqualTo("9/8");
  }

  @Test
  public void testAdditionNumbers() {
    ExtendedRational a = new ExtendedRational(Rational.ofLongs(12, 8));
    ExtendedRational b = new ExtendedRational(Rational.ofLongs(-54, 12));
    assertThat(a.plus(b)).isEqualTo(new ExtendedRational(Rational.ofLong(-3)));
  }

  @Test
  public void testAdditionInfty() {
    ExtendedRational a = new ExtendedRational(Rational.ofLongs(12, 8));
    ExtendedRational b = ExtendedRational.INFTY;
    assertThat(a.plus(b)).isEqualTo(ExtendedRational.INFTY);
  }

  @Test
  public void testAdditionNaN() {
    ExtendedRational a = new ExtendedRational(Rational.ofLongs(12, 8));
    ExtendedRational b = ExtendedRational.NaN;
    assertThat(a.plus(b)).isEqualTo(ExtendedRational.NaN);
  }

  @Test
  public void testAdditionInfinities() {
    ExtendedRational a = ExtendedRational.INFTY;
    ExtendedRational b = ExtendedRational.NEG_INFTY;
    assertThat(a.plus(b)).isEqualTo(ExtendedRational.NaN);
  }

  @Test
  public void testAdditionNegInfty() {
    ExtendedRational a = ExtendedRational.ofString("2309820938409238490");
    ExtendedRational b = ExtendedRational.NEG_INFTY;
    assertThat(a.plus(b)).isEqualTo(ExtendedRational.NEG_INFTY);
  }

  @Test
  public void testSubtraction() {
    ExtendedRational a = ExtendedRational.ofString("5/2");
    ExtendedRational b = ExtendedRational.ofString("3/2");
    assertThat(a.minus(b)).isEqualTo(new ExtendedRational(Rational.ofLong(1)));
  }

  @Test
  public void testMultiplication1() {
    ExtendedRational a = ExtendedRational.ofString("2/4");
    ExtendedRational b = ExtendedRational.ofString("-1/3");
    assertThat(a.times(b)).isEqualTo(new ExtendedRational(Rational.ofLongs(-2, 12)));
  }

  @Test
  public void testMultiplication2() {
    ExtendedRational a = ExtendedRational.ofString("100/4");
    ExtendedRational b = ExtendedRational.ofString("1/100");
    assertThat(a.times(b)).isEqualTo(new ExtendedRational(Rational.ofLongs(1, 4)));
  }

  @Test
  public void testMultiplicationInfty() {
    ExtendedRational a = ExtendedRational.ofString("100/4");
    ExtendedRational b = ExtendedRational.ofString("Infinity");
    assertThat(a.times(b)).isEqualTo(ExtendedRational.INFTY);
  }

  @Test
  public void testMultiplicationZero() {
    ExtendedRational a = ExtendedRational.ZERO;
    ExtendedRational b = ExtendedRational.NEG_INFTY;
    assertThat(a.times(b)).isEqualTo(ExtendedRational.ZERO);

    a = ExtendedRational.ZERO;
    b = ExtendedRational.INFTY;
    assertThat(a.times(b)).isEqualTo(ExtendedRational.ZERO);
  }

  @Test
  public void testMultiplicationNegInfty() {
    ExtendedRational a = ExtendedRational.NEG_INFTY;
    ExtendedRational b = ExtendedRational.NEG_INFTY;
    assertThat(a.times(b)).isEqualTo(ExtendedRational.INFTY);

    b = ExtendedRational.INFTY;
    assertThat(a.times(b)).isEqualTo(ExtendedRational.NEG_INFTY);
  }

  @Test
  public void testGetRational() {
    ExtendedRational a = ExtendedRational.ofString("3/4");
    assertThat(a.getRational()).isEqualTo(Rational.ofString("3/4"));
  }

  @Test
  public void testDivisionNumber() {
    ExtendedRational a = ExtendedRational.ofString("2/4");
    ExtendedRational b = ExtendedRational.ofString("1/4");
    assertThat(a.divides(b)).isEqualTo(new ExtendedRational(Rational.ofLong(2)));
  }

  @Test
  public void testDivisionInfty() {
    ExtendedRational a = ExtendedRational.ofString("234234");
    ExtendedRational b = ExtendedRational.INFTY;
    assertThat(a.divides(b)).isEqualTo(new ExtendedRational(Rational.ofLong(0)));
  }

  @Test
  public void testDivisionNegInfty() {
    ExtendedRational a = ExtendedRational.ofString("234234");
    ExtendedRational b = ExtendedRational.NEG_INFTY;
    assertThat(a.divides(b)).isEqualTo(new ExtendedRational(Rational.ofLong(0)));
  }

  @Test
  public void testDivisionNaN() {
    ExtendedRational a = ExtendedRational.ofString("234234");
    ExtendedRational b = ExtendedRational.NaN;
    assertThat(a.divides(b)).isEqualTo(ExtendedRational.NaN);
  }

  @Test
  public void testComparison() {
    List<ExtendedRational> unsorted =
        Arrays.asList(
            ExtendedRational.NaN,
            ExtendedRational.NEG_INFTY,
            ExtendedRational.ofString("-2/4"),
            ExtendedRational.ofString("1/3"),
            ExtendedRational.ofString("2/3"),
            ExtendedRational.INFTY);
    Collections.shuffle(unsorted);

    List<ExtendedRational> sorted =
        Arrays.asList(
            ExtendedRational.NEG_INFTY,
            ExtendedRational.ofString("-2/4"),
            ExtendedRational.ofString("1/3"),
            ExtendedRational.ofString("2/3"),
            ExtendedRational.INFTY,
            ExtendedRational.NaN);

    Collections.sort(unsorted);

    assertThat(unsorted).containsExactlyElementsIn(sorted).inOrder();
  }

  @Test
  public void testOfStringInfty() {
    ExtendedRational a = ExtendedRational.ofString("Infinity");
    assertThat(a).isEqualTo(ExtendedRational.INFTY);
  }

  @Test
  public void testOfStringNegInfty() {
    ExtendedRational a = ExtendedRational.ofString("-Infinity");
    assertThat(a).isEqualTo(ExtendedRational.NEG_INFTY);
  }

  @Test
  public void testOfStringNaN() {
    ExtendedRational a = ExtendedRational.ofString("NaN");
    assertThat(a).isEqualTo(ExtendedRational.NaN);
  }

  @Test
  public void testOfStringNumber() {
    ExtendedRational a = ExtendedRational.ofString("-2");
    Assert.assertEquals(new ExtendedRational(Rational.ofLong(-2)), a);
  }
}
