package org.sosy_lab.common.rationals;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.annotation.Nullable;

/**
 * Rational class, throws {@link IllegalArgumentException} on unsupported
 * operations (e.g. {@code 1/0}).
 *
 * <p>The Rational object is immutable.
 * All arithmetic operations return new instances.
 *
 * <p>For performance and convenience, there is always only a single {@code Rational}
 * instance representing numbers 0, 1 and -1.
 * These numbers can be compared using {@code ==} operator.
 */
@SuppressWarnings("NumberEquality")
public final class Rational extends Number implements Comparable<Rational> {

  private static final long serialVersionUID = 1657347377738275521L;

  // -- Just some shortcuts for BigIntegers --
  private static final BigInteger B_ZERO = BigInteger.ZERO;
  private static final BigInteger B_ONE = BigInteger.ONE;
  private static final BigInteger B_M_ONE = B_ONE.negate();

  public static final Rational ZERO = new Rational(B_ZERO, B_ONE);
  public static final Rational ONE = new Rational(B_ONE, B_ONE);
  public static final Rational NEG_ONE = new Rational(B_M_ONE, B_ONE);

  @Nullable private transient String stringCache = null;

  /**
   * Rationals are always stored in the normal form.
   * That is:
   *
   * a) denominator is strictly positive.
   * b) numerator and denominator do not have common factors.
   * c) 0, 1 and -1 have unique representation corresponding to the
   * class static constants ZERO, ONE and NEG_ONE. That is, they can be
   * compared using the '==' operator.
   */
  private final BigInteger num;
  private final BigInteger den;

  private Rational(BigInteger numerator, BigInteger denominator) {
    num = numerator;
    den = denominator;
    assert den.signum() == 1;
  }

  /** Factory functions **/

  /**
   * Create a new rational from a numerator and a denominator.
   */
  public static Rational of(BigInteger numerator, BigInteger denominator) {
    checkNotNull(numerator);
    int denSignum = denominator.signum();
    if (denSignum == 0) {
      throw new IllegalArgumentException("Infinity is not supported, use ExtendedRational instead");
    }

    if (denSignum == -1) {
      // Make {@code denominator} positive.
      denominator = denominator.negate();
      numerator = numerator.negate();
    }

    // Reduce by GCD. GCD will never be zero as the denominator is never
    // zero at this stage.
    BigInteger gcd = numerator.gcd(denominator);
    numerator = numerator.divide(gcd);
    denominator = denominator.divide(gcd);

    return ofNormalForm(numerator, denominator);
  }

  /**
   * Wrapper around the constructor, returns cached constants if possible.
   * Assumes that <code>num</code> and <code>den</code> are in the normal form.
   */
  private static Rational ofNormalForm(BigInteger num, BigInteger den) {
    if (num.equals(B_ZERO)) {
      return ZERO;
    } else if (den.equals(B_ONE)) {
      if (num.equals(B_ONE)) {
        return ONE;
      } else if (num.equals(B_M_ONE)) {
        return NEG_ONE;
      }
    }
    return new Rational(num, den);
  }

  /**
   * Create a new rational from two longs.
   */
  public static Rational ofLongs(long numerator, long denominator) {
    return of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
  }

  /**
   * Create a new rational equal to the given long.
   */
  public static Rational ofLong(long numerator) {
    return of(BigInteger.valueOf(numerator), B_ONE);
  }

  /**
   * Create a new rational equal to the given BigInteger.
   */
  public static Rational ofBigInteger(BigInteger numerator) {
    return of(numerator, B_ONE);
  }

  /**
   * Reverses the effect of {@link Rational#toString}.
   * Supports 2 different formats: with slash (e.g. {@code 25/17})
   * or without slash (e.g. {@code 5})
   *
   * @param s Input string
   * @throws NumberFormatException iff {@code s} is not a valid representation
   * of Rational.
   * @throws IllegalArgumentException If the resulting rational is undefined (e.g. 0/0 or 1/0).
   */
  public static Rational ofString(String s) throws NumberFormatException {
    int idx = s.indexOf('/');
    BigInteger num;
    BigInteger den;
    if (idx == -1) { // No slash found.
      num = new BigInteger(s);
      return ofBigInteger(num);
    } else {
      num = new BigInteger(s.substring(0, idx));
      den = new BigInteger(s.substring(idx + 1, s.length()));
      return of(num, den);
    }
  }

  /**
   * Syntax sugar helper for creating Rationals.
   *
   * @see #ofString(String)
   */
  public static Rational of(String s) throws NumberFormatException {
    return ofString(s);
  }

  /**
   * Syntax sugar.
   *
   * @see #ofLong(long)
   */
  public static Rational of(long l) throws NumberFormatException {
    return ofLong(l);
  }

  /**
   * Convert a given BigDecimal to Rational.
   */
  public static Rational ofBigDecimal(BigDecimal decimal) {
    if (decimal.scale() <= 0) {
      BigInteger num = decimal.toBigInteger();
      return Rational.of(num, BigInteger.ONE);
    } else {
      BigInteger num = decimal.unscaledValue();
      BigInteger denom = BigInteger.TEN.pow(decimal.scale());
      return Rational.of(num, denom);
    }
  }


  /**
   * Multiply by {@code b}, return a new instance.
   **/
  public Rational times(Rational b) {
    checkNotNull(b);
    Rational a = this;
    if (a == ZERO || b == ZERO) {
      return ZERO;
    }
    if (a == ONE) {
      return b;
    }
    if (b == ONE) {
      return a;
    }

    // reduce p1/q2 and p2/q1, then multiply, where a = p1/q1 and b = p2/q2
    Rational c = of(a.num, b.den);
    Rational d = of(b.num, a.den);
    return ofNormalForm(c.num.multiply(d.num), c.den.multiply(d.den));
  }

  /**
   * Return a new instance equal to the sum of {@code this} and {@code b}.
   */
  public Rational plus(Rational b) {
    checkNotNull(b);
    Rational a = this;
    if (a == ZERO) {
      return b;
    }
    if (b == ZERO) {
      return a;
    }

    return of((a.num.multiply(b.den).add(b.num.multiply(a.den))), a.den.multiply(b.den));
  }

  /**
   * Return a new instance equal to {@code this - b}.
   */
  public Rational minus(Rational b) {
    return plus(b.negate());
  }

  /**
   * Return {@code this / b}.
   */
  public Rational divides(Rational b) {
    // Reciprocal method will throw the exception for the division-by-zero
    // error if required.
    return times(b.reciprocal());
  }

  /**
   * Return reciprocal of {@code this}.
   *
   * @throws IllegalArgumentException If invoked on zero.
   */
  public Rational reciprocal() throws IllegalArgumentException {
    if (num.equals(B_ZERO)) {
      throw new IllegalArgumentException(
          "Division by zero not supported, use ExtendedRational if you need it");
    }
    return of(den, num);
  }

  /**
   * Return negation of {@code this}.
   */
  public Rational negate() {
    return ofNormalForm(num.negate(), den);
  }

  @Override
  public double doubleValue() {
    return num.doubleValue() / den.doubleValue();
  }

  public boolean isIntegral() {
    return den.equals(B_ONE);
  }

  public BigInteger getNum() {
    return num;
  }

  public BigInteger getDen() {
    return den;
  }

  /**
   * @return -1, 0 or 1, representing the sign of the rational number.
   */
  public int signum() {
    return num.signum();
  }

  /**
   * @return String of the form num/den.
   */
  @Override
  public String toString() {
    if (stringCache == null) {
      if (den.equals(B_ONE)) {
        stringCache = num.toString();
      } else {
        stringCache = num + "/" + den;
      }
    }
    return stringCache;
  }

  @Override
  public int compareTo(Rational b) {
    BigInteger lhs = num.multiply(b.den);
    BigInteger rhs = den.multiply(b.num);
    return lhs.subtract(rhs).signum();
  }

  @Override
  public boolean equals(@Nullable Object y) {
    if (this == y) {
      return true;
    }
    if (y == null) {
      return false;
    }
    if (y.getClass() != this.getClass()) {
      return false;
    }
    Rational b = (Rational) y;
    return (num.equals(b.num) && den.equals(b.den));
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(num, den);
  }

  public static Rational max(Rational a, Rational b) {
    if (a.compareTo(b) >= 0) {
      return a;
    }
    return b;
  }

  @Override
  public int intValue() {
    return (int) doubleValue();
  }

  @Override
  public long longValue() {
    return (long) doubleValue();
  }

  @Override
  public float floatValue() {
    return (float) doubleValue();
  }
}
