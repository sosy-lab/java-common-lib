package org.sosy_lab.common.rationals.tests;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.testing.EqualsTester;
import com.google.errorprone.annotations.Var;
import org.junit.Test;
import org.sosy_lab.common.rationals.LinearExpression;
import org.sosy_lab.common.rationals.Rational;

public class LinearExpressionTest {

  @Test
  public void testInstantiationEmpty() {
    LinearExpression<String> x = LinearExpression.empty();
    assertThat(x.size()).isEqualTo(0);
  }

  @Test
  public void testInstantiationPair() {
    LinearExpression<String> x = LinearExpression.monomial("x", Rational.ofString("5"));
    assertThat(x.size()).isEqualTo(1);
    assertThat(x.getCoeff("x")).isEqualTo(Rational.ofString("5"));
  }

  @Test
  public void testInstantiationVariable() {
    LinearExpression<String> x = LinearExpression.ofVariable("y");
    assertThat(x.size()).isEqualTo(1);
    assertThat(x.getCoeff("y")).isEqualTo(Rational.ONE);
  }

  @Test
  public void testInstantiationPairZero() {
    LinearExpression<String> x = LinearExpression.monomial("x", Rational.ofString("0"));
    assertThat(x.size()).isEqualTo(0);
  }

  @Test
  public void testAdd() {
    @Var LinearExpression<String> x = LinearExpression.monomial("x", Rational.ofString("5"));
    x = x.add(LinearExpression.monomial("x", Rational.ofString("8")));
    x = x.add(LinearExpression.monomial("y", Rational.ofString("2")));
    x = x.add(LinearExpression.monomial("z", Rational.ofString("3")));
    assertThat(x.size()).isEqualTo(3);
    assertThat(x.getCoeff("x")).isEqualTo(Rational.ofString("13"));
    assertThat(x.getCoeff("y")).isEqualTo(Rational.ofString("2"));
    assertThat(x.getCoeff("z")).isEqualTo(Rational.ofString("3"));

    assertThat(x.isIntegral()).isTrue();
  }

  @Test
  public void testSub() {
    @Var LinearExpression<String> x = LinearExpression.monomial("x", Rational.ofString("5"));
    x = x.add(LinearExpression.monomial("y", Rational.ofString("3")));
    x = x.sub(LinearExpression.monomial("x", Rational.ofString("5")));
    x = x.sub(LinearExpression.monomial("y", Rational.ofString("2")));
    x = x.sub(LinearExpression.monomial("z", Rational.ofString("1")));

    assertThat(x.size()).isEqualTo(2);
    assertThat(x.getCoeff("x")).isEqualTo(Rational.ZERO);
    assertThat(x.getCoeff("y")).isEqualTo(Rational.ONE);
    assertThat(x.getCoeff("z")).isEqualTo(Rational.NEG_ONE);
  }

  @Test
  public void testMultiplication1() {
    @Var LinearExpression<String> x = LinearExpression.monomial("x", Rational.ofString("5"));
    x = x.multByConst(Rational.ZERO);
    assertThat(x.size()).isEqualTo(0);
  }

  @Test
  public void testMultiplication2() {
    @Var LinearExpression<String> x = LinearExpression.monomial("x", Rational.ofString("5"));
    x = x.add(LinearExpression.monomial("y", Rational.ofString("3")));
    x = x.multByConst(Rational.ofString("2"));
    assertThat(x.size()).isEqualTo(2);
    assertThat(x.getCoeff("x")).isEqualTo(Rational.ofString("10"));
    assertThat(x.getCoeff("y")).isEqualTo(Rational.ofString("6"));
  }

  @Test
  public void testDivisionOK() {
    LinearExpression<String> num =
        LinearExpression.monomial("x", Rational.ofString("3"))
            .add(LinearExpression.monomial("y", Rational.ofString("6")));
    LinearExpression<String> den =
        LinearExpression.monomial("x", Rational.ofString("1"))
            .add(LinearExpression.monomial("y", Rational.ofString("2")));
    assertThat(num.divide(den)).hasValue(Rational.ofString("3"));
  }

  @Test
  public void testDivisionNotPossible() {
    LinearExpression<String> num =
        LinearExpression.monomial("x", Rational.ofString("3"))
            .add(LinearExpression.monomial("y", Rational.ofString("7")));
    LinearExpression<String> den =
        LinearExpression.monomial("x", Rational.ofString("1"))
            .add(LinearExpression.monomial("y", Rational.ofString("2")));
    assertThat(num.divide(den)).isEmpty();
  }

  @Test
  public void testNegation() {
    @Var LinearExpression<String> x = LinearExpression.monomial("x", Rational.ofString("5"));
    x = x.add(LinearExpression.monomial("y", Rational.ofString("3")));
    x = x.negate();
    assertThat(x.size()).isEqualTo(2);
    assertThat(x.getCoeff("x")).isEqualTo(Rational.ofString("-5"));
    assertThat(x.getCoeff("y")).isEqualTo(Rational.ofString("-3"));
  }

  @Test
  public void testEquality() {
    LinearExpression<String> x = LinearExpression.monomial("x", Rational.ofString("6"));
    @Var LinearExpression<String> y = LinearExpression.monomial("x", Rational.ofString("3"));
    y = y.add(LinearExpression.monomial("x", Rational.ofString("3")));
    y = y.add(LinearExpression.monomial("z", Rational.ofString("3")));
    y = y.sub(LinearExpression.monomial("z", Rational.ofString("3")));

    new EqualsTester().addEqualityGroup(x, y).testEquals();
  }
}
