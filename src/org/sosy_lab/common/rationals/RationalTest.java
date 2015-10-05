package org.sosy_lab.common.rationals;

import static com.google.common.truth.Truth.assertThat;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class RationalTest {
  @Test
  public void testInstantiationLongs() {
    Rational x = Rational.ofLongs(108, 96);
    assertThat(x.toString()).isEqualTo("9/8");
  }

  @Test
  public void testInstantiationLong() {
    Rational x = Rational.ofLong(50);
    assertThat(x.toString()).isEqualTo("50");
  }

  @Test
  public void testInstantiationBigIntegers() {
    Rational x = Rational.of(BigInteger.ZERO, BigInteger.ONE);
    assertThat(x).isEqualTo(Rational.ZERO);
  }

  @Test
  public void testAddition() {
    Rational a = Rational.ofLongs(12, 8);
    Rational b = Rational.ofLongs(-54, 12);
    assertThat(a.plus(b)).isEqualTo(Rational.ofLong(-3));
  }

  @Test
  public void testSubtraction() {
    Rational a = Rational.ofString("5/2");
    Rational b = Rational.ofString("3/2");
    assertThat(a.minus(b)).isEqualTo(Rational.ofLong(1));
  }

  @Test
  public void testMultiplication1() {
    Rational a = Rational.ofString("2/4");
    Rational b = Rational.ofString("-1/3");
    assertThat(a.times(b)).isEqualTo(Rational.ofLongs(-2, 12));
  }

  @Test
  public void testMultiplication2() {
    Rational a = Rational.ofString("100/4");
    Rational b = Rational.ofString("1/100");
    assertThat(a.times(b)).isEqualTo(Rational.ofLongs(1, 4));
  }

  @Test
  public void testDivision() {
    Rational a = Rational.ofString("2/4");
    Rational b = Rational.ofString("1/4");
    assertThat(a.divides(b)).isEqualTo(Rational.ofLong(2));
  }

  @Test
  public void testComparison() {
    List<Rational> unsorted = Arrays.asList(
        Rational.ofLongs(-2, 4),
        Rational.ofLongs(1, 3),
        Rational.ofLongs(2, 3)
    );
    Collections.shuffle(unsorted);

    List<Rational> sorted = Arrays.asList(
        Rational.ofLongs(-2, 4),
        Rational.ofLongs(1, 3),
        Rational.ofLongs(2, 3)
    );

    Collections.sort(unsorted);

    assertThat(unsorted).containsExactlyElementsIn(sorted).inOrder();
  }

  @Test
  public void testOfString1() {
    Rational a = Rational.ofString("6/8");
    assertThat(a).isEqualTo(Rational.ofLongs(3, 4));
  }

  @Test
  public void testOfString2() {
    Rational a = Rational.ofString("-2");
    assertThat(a).isEqualTo(Rational.ofLongs(-2, 1));
  }

  @Test
  public void testCanonicity1() {
    Rational a = Rational.ofString("6/8");
    Rational b = Rational.ofString("-6/8");
    assertThat(a.plus(b)).isSameAs(Rational.ZERO);
  }

  @Test
  public void testCanonicity2() {
    Rational x = Rational.ofString("-1");
    assertThat(x).isSameAs(Rational.NEG_ONE);
  }

  @Test
  public void testCanonicity3() {
    Rational a = Rational.ofString("2");
    Rational b = Rational.ofString("-1");
    assertThat(a.plus(b)).isSameAs(Rational.ONE);
  }

  @Test
  public void testCanonicity4() {
    Rational a = Rational.ofString("-2");
    Rational b = Rational.ofString("1");
    assertThat(a.plus(b)).isSameAs(Rational.NEG_ONE);
  }
}
