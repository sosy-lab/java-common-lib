// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Var;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Utility class for measuring how similar two strings are, e.g. to suggest a likely-intended value
 * when a user-supplied string (a configuration option name, a command-line argument, ...) does not
 * match any known value exactly.
 */
public final class StringSimilarity {
  private StringSimilarity() {}

  /**
   * Returns a similarity score between {@code a} and {@code b} in the range {@code [0.0, 1.0]},
   * based on the normalized Optimal String Alignment distance (a restricted variant of the
   * Damerau-Levenshtein edit distance that additionally counts a transposition of two adjacent
   * characters as a single edit).
   *
   * <p>The raw edit distance is normalized as {@code 1 - distance / max(a.length(), b.length())},
   * so identical strings score {@code 1.0} and, as a rule of thumb, {@code 0.8} to {@code 0.99}
   * means very similar (e.g. a single typo), {@code 0.6} to {@code 0.79} means moderately similar,
   * and below {@code 0.6} means the strings are probably unrelated. These thresholds depend on
   * string length and should be tuned per use case. Examples:
   *
   * <pre>{@code
   * damerauLevenshteinSimilarity("termination", "termination") == 1.0
   * damerauLevenshteinSimilarity("memsafety", "memorysafety") == 0.75
   * damerauLevenshteinSimilarity("", "") == 1.0
   * }</pre>
   *
   * <p>This method uses the <em>restricted</em> Optimal String Alignment variant rather than "true"
   * Damerau-Levenshtein distance: once a pair of adjacent characters has been transposed, that pair
   * may not be edited again. This is simpler to implement correctly (no auxiliary
   * last-seen-position bookkeeping) and is indistinguishable from true Damerau-Levenshtein for
   * realistic single-word typos; it only under-counts in adversarial cases with overlapping
   * transpositions (e.g. transforming {@code "CA"} into {@code "ABC"}), which do not occur for
   * option-name-style inputs.
   *
   * @param a the first string, must not be {@code null}
   * @param b the second string, must not be {@code null}
   * @return a similarity score in {@code [0.0, 1.0]}
   */
  public static double damerauLevenshteinSimilarity(String a, String b) {
    checkNotNull(a);
    checkNotNull(b);
    int maxLength = Math.max(a.length(), b.length());
    if (maxLength == 0) {
      return 1.0;
    }
    return 1.0 - (double) optimalStringAlignmentDistance(a, b) / maxLength;
  }

  /**
   * Returns the elements of {@code candidates} whose {@link #damerauLevenshteinSimilarity} to
   * {@code stringToCompareTo} is at least {@code threshold}, sorted by descending similarity.
   * Candidates with equal similarity keep their relative order from {@code candidates} (stable
   * sort). Duplicate candidates are not removed.
   *
   * @param stringToCompareTo the string to find close matches for, must not be {@code null}
   * @param candidates the pool of possible values to search, must not be {@code null}
   * @param threshold the minimum similarity score (inclusive) a candidate must reach to be included
   * @return an immutable list of matching candidates, ordered from closest to least close
   */
  public static ImmutableList<String> findClosestMatches(
      String stringToCompareTo, Collection<String> candidates, double threshold) {
    checkNotNull(stringToCompareTo);
    checkNotNull(candidates);

    List<SimpleImmutableEntry<String, Double>> scoredMatches = new ArrayList<>();
    for (String candidate : candidates) {
      double score = damerauLevenshteinSimilarity(stringToCompareTo, candidate);
      if (score >= threshold) {
        scoredMatches.add(new SimpleImmutableEntry<>(candidate, score));
      }
    }
    // List.sort() is a stable sort, so candidates with equal scores keep their relative order.
    scoredMatches.sort(
        Comparator.comparingDouble(SimpleImmutableEntry<String, Double>::getValue).reversed());

    ImmutableList.Builder<String> result = ImmutableList.builder();
    for (SimpleImmutableEntry<String, Double> entry : scoredMatches) {
      result.add(entry.getKey());
    }
    return result.build();
  }

  /**
   * Computes the Optimal String Alignment distance between {@code a} and {@code b}: the minimum
   * number of insertions, deletions, substitutions, and adjacent-character transpositions needed to
   * turn {@code a} into {@code b}, where a transposed pair of characters may not be edited again
   * afterwards.
   */
  private static int optimalStringAlignmentDistance(String stringA, String stringB) {
    int lengthA = stringA.length();
    int lengthB = stringB.length();

    int[][] distances = new int[lengthA + 1][lengthB + 1];

    for (int indexA = 0; indexA <= lengthA; indexA++) {
      distances[indexA][0] = indexA;
    }

    for (int indexB = 0; indexB <= lengthB; indexB++) {
      distances[0][indexB] = indexB;
    }

    for (int indexA = 1; indexA <= lengthA; indexA++) {
      for (int indexB = 1; indexB <= lengthB; indexB++) {
        int substitutionCost = stringA.charAt(indexA - 1) == stringB.charAt(indexB - 1) ? 0 : 1;
        int deletionCost = distances[indexA - 1][indexB] + 1;
        int insertionCost = distances[indexA][indexB - 1] + 1;
        int substitutionTotalCost = distances[indexA - 1][indexB - 1] + substitutionCost;

        @Var int minCost = Math.min(deletionCost, Math.min(insertionCost, substitutionTotalCost));

        boolean isTransposition =
            indexA > 1
                && indexB > 1
                && stringA.charAt(indexA - 1) == stringB.charAt(indexB - 2)
                && stringA.charAt(indexA - 2) == stringB.charAt(indexB - 1);

        if (isTransposition) {
          int transpositionCost = distances[indexA - 2][indexB - 2] + 1;
          minCost = Math.min(minCost, transpositionCost);
        }

        distances[indexA][indexB] = minCost;
      }
    }

    return distances[lengthA][lengthB];
  }
}
