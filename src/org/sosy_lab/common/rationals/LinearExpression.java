package org.sosy_lab.common.rationals;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.Var;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Simple <i>sparse</i> implementation for <i>homogeneous</i> linear expression of the form $\Sigma
 * a_i x_i$, where $x_i$ is a set of variables and $a_i$ is a set of constants.
 *
 * <p>Every constant stored has to have a non-zero value.
 */
@Immutable(containerOf = "T")
public final class LinearExpression<T> implements Iterable<Entry<T, Rational>> {
  private final ImmutableMap<T, Rational> data;
  private transient @LazyInit int hashCache = 0;

  private LinearExpression(Map<T, Rational> data) {
    this.data = ImmutableMap.copyOf(data);
  }

  /** Creates an empty linear expression. */
  public static <T> LinearExpression<T> empty() {
    return new LinearExpression<>(ImmutableMap.of());
  }

  /**
   * Return a monomial with a variable {@code var} and a coefficient {@code coeff}.
   *
   * @deprecated Use {@link #monomial} instead.
   */
  @Deprecated
  public static <T> LinearExpression<T> pair(T var, Rational coeff) {
    return monomial(var, coeff);
  }

  /** Return a monomial with a variable {@code var} and a coefficient {@code coeff}. */
  public static <T> LinearExpression<T> monomial(T var, Rational coeff) {
    if (coeff.equals(Rational.ZERO)) {
      return empty();
    }
    return new LinearExpression<>(ImmutableMap.of(var, coeff));
  }

  /** Create a monomial consisting of variable {@code var} with a coefficient of one. */
  public static <T> LinearExpression<T> ofVariable(T var) {
    return LinearExpression.monomial(var, Rational.ONE);
  }

  /** Add {@code other} linear expression. */
  public LinearExpression<T> add(LinearExpression<T> other) {
    ImmutableMap.Builder<T, Rational> resultData = ImmutableMap.builder();
    for (Entry<T, Rational> e : data.entrySet()) {
      T var = e.getKey();
      @Var Rational value = e.getValue();
      Rational otherValue = other.data.get(var);
      if (otherValue != null) {
        value = value.plus(otherValue);
      }
      if (!value.equals(Rational.ZERO)) {
        resultData.put(var, value);
      }
    }
    for (Entry<T, Rational> e : other.data.entrySet()) {
      T var = e.getKey();
      if (!data.containsKey(var)) {
        resultData.put(e.getKey(), e.getValue());
      }
    }
    return new LinearExpression<>(resultData.build());
  }

  /** Subtract {@code other} linear expression. */
  public LinearExpression<T> sub(LinearExpression<T> other) {
    return add(other.negate());
  }

  /** Multiply the linear expression by {@code constant}. */
  public LinearExpression<T> multByConst(Rational constant) {
    if (constant.equals(Rational.ZERO)) {
      return empty();
    }
    return new LinearExpression<>(Maps.transformValues(data, value -> value.times(constant)));
  }
  /** Negate the linear expression. */
  public LinearExpression<T> negate() {
    return multByConst(Rational.NEG_ONE);
  }

  public Rational getCoeff(T variable) {
    checkNotNull(variable);
    Rational out = data.get(variable);
    if (out == null) {
      return Rational.ZERO;
    }
    return out;
  }

  /** @return Number of variables with non-zero coefficients. */
  public int size() {
    return data.size();
  }

  public boolean isEmpty() {
    return data.isEmpty();
  }

  /** @return Whether all coefficients are integral. */
  public boolean isIntegral() {
    return data.values().stream().allMatch(Rational::isIntegral);
  }

  /**
   * @return {@code a} iff {@code other.multByConst(a) == this}, {@code Optional.absent()} if no
   *     such constant exists.
   */
  public Optional<Rational> divide(LinearExpression<T> other) {
    if (other.size() != data.size()) {
      return Optional.empty();
    }
    @Var Rational multiplier = null;
    for (T key : data.keySet()) {
      Rational div = other.getCoeff(key).divides(data.get(key)).reciprocal();
      if (multiplier == null) {
        multiplier = div;
      } else if (!multiplier.equals(div)) {
        return Optional.empty();
      }
    }
    assert multiplier != null;
    return Optional.of(multiplier);
  }

  @Override
  public Iterator<Entry<T, Rational>> iterator() {
    return data.entrySet().iterator();
  }

  /** @return The underlying stored map. */
  public ImmutableMap<T, Rational> getMap() {
    return data;
  }

  /** @return Pretty-printing for linear expressions. E. g. <i>-x + 2y + z</i> */
  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    data.forEach((var, coeff) -> writeMonomial(var.toString(), coeff, b));
    return b.toString();
  }

  /** Pretty-print monomial to the given {@link StringBuilder}. */
  @SuppressWarnings("ReferenceEquality") // normalized form uses cached instances
  public static void writeMonomial(String varSerialized, Rational coeff, StringBuilder b) {
    checkNotNull(varSerialized);
    if (b.length() != 0 && coeff.signum() >= 0) {
      b.append(" + ");
    }
    if (coeff == Rational.ONE) {
      b.append(varSerialized);
    } else if (coeff == Rational.NEG_ONE) {
      b.append(" - ").append(varSerialized);
    } else {
      b.append(coeff.toString()).append(varSerialized);
    }
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object == this) {
      return true;
    }
    if (object == null) {
      return false;
    }
    if (object.getClass() != this.getClass()) {
      return false;
    }
    LinearExpression<?> other = (LinearExpression<?>) object;
    return data.equals(other.data);
  }

  @Override
  public int hashCode() {
    // Caching the hashing procedure.
    if (hashCache == 0) {
      hashCache = data.hashCode();
    }

    // Safe to do so, since we are immutable.
    return hashCache;
  }
}
