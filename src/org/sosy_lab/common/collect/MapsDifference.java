/*
 *  SoSy-Lab Common is a library of useful utilities.
 *  This file is part of SoSy-Lab Common.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auto.value.AutoValue;
import com.google.errorprone.annotations.Immutable;

import java.util.Collection;
import java.util.Optional;

/**
 * Utilities for handling differences between maps.
 */
public class MapsDifference {

  private MapsDifference() {}

  /**
   * Returns a default {@link Visitor} that does nothing.
   * Use this if a method requires a visitor to be passed
   * but you are not interested in the elements.
   */
  @SuppressWarnings("unchecked")
  public static <K, V> Visitor<K, V> ignoreMapsDifference() {
    return (Visitor<K, V>) DummyVisitor.INSTANCE;
  }

  /**
   * Returns a {@link Visitor} that collects all map differences
   * as {@link Entry} objects into a given collection.
   */
  public static <K, V> Visitor<K, V> collectMapsDifferenceTo(final Collection<Entry<K, V>> target) {
    checkNotNull(target);
    return new Visitor<K, V>() {
      @Override
      public void leftValueOnly(K pKey, V pLeftValue) {
        target.add(Entry.forLeftValueOnly(pKey, pLeftValue));
      }

      @Override
      public void rightValueOnly(K pKey, V pRightValue) {
        target.add(Entry.forRightValueOnly(pKey, pRightValue));
      }

      @Override
      public void differingValues(K pKey, V pLeftValue, V pRightValue) {
        target.add(Entry.forDifferingValues(pKey, pLeftValue, pRightValue));
      }
    };
  }

  /**
   * Interface for visiting map entries differing between two maps.
   *
   * If you are interested in a visitor that collects all passed elements,
   * please use {@link MapsDifference#collectMapsDifferenceTo(Collection)}.
   * A dummy implementation that does nothing is available from
   * {@link MapsDifference#ignoreMapsDifference()}. For implementing your own visitor,
   * you can inherit from {@link DefaultVisitor}.
   *
   * @param <K> The type of the key.
   * @param <V> The type of the values.
   */
  public interface Visitor<K, V> {

    void leftValueOnly(K key, V leftValue);

    void rightValueOnly(K key, V rightValue);

    /**
     * Accept a map difference.
     * @param key The key.
     * @param leftValue The left value.
     * @param rightValue The right value.
     */
    void differingValues(K key, V leftValue, V rightValue);
  }

  /**
   * Default implementation of {@link Visitor} with empty methods.
   */
  public abstract static class DefaultVisitor<K, V> implements Visitor<K, V> {

    @Override
    public void leftValueOnly(K pKey, V pLeftValue) {}

    @Override
    public void rightValueOnly(K key, V rightValue) {}

    @Override
    public void differingValues(K pKey, V pLeftValue, V pRightValue) {}
  }

  private enum DummyVisitor implements Visitor<Object, Object> {
    INSTANCE;

    @Override
    public void leftValueOnly(Object pKey, Object pLeftValue) {}

    @Override
    public void rightValueOnly(Object key, Object rightValue) {}

    @Override
    public void differingValues(Object pKey, Object pLeftValue, Object pRightValue) {}
  }

  /**
   * Class representing the difference between two maps for a given key.
   * This class only allows non-null keys and values.
   * @param <K> The type of the key.
   * @param <V> The type of the values.
   */
  @AutoValue
  @Immutable(containerOf={"K", "V"})
  public abstract static class Entry<K, V> {

    Entry() {}

    public static <K, V> Entry<K, V> forLeftValueOnly(K pKey, V pLeftValue) {
      return new AutoValue_MapsDifference_Entry<>(pKey, Optional.of(pLeftValue), Optional.empty());
    }

    public static <K, V> Entry<K, V> forRightValueOnly(K pKey, V pRightValue) {
      return new AutoValue_MapsDifference_Entry<>(pKey, Optional.empty(), Optional.of(pRightValue));
    }

    public static <K, V> Entry<K, V> forDifferingValues(K pKey, V pLeftValue, V pRightValue) {
      return new AutoValue_MapsDifference_Entry<>(
          pKey, Optional.of(pLeftValue), Optional.of(pRightValue));
    }

    /**
     * Returns the map key.
     */
    public abstract K getKey();

    /**
     * Returns the left value, if present.
     */
    public abstract Optional<V> getLeftValue();

    /**
     * Returns the right value, if present.
     */
    public abstract Optional<V> getRightValue();
  }
}
