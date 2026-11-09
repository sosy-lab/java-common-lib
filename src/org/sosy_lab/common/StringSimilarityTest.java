// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class StringSimilarityTest {

  @Test
  public void similarity_identical() {
    assertThat(StringSimilarity.damerauLevenshteinSimilarity("termination", "termination"))
        .isEqualTo(1.0);
  }

  @Test
  public void similarity_transposition() {
    // "memorysafety" -> "memroysafety": adjacent swap of 'o' and 'r'.
    assertThat(StringSimilarity.damerauLevenshteinSimilarity("memorysafety", "memroysafety"))
        .isEqualTo(1.0 - 1.0 / 12);
  }

  @Test
  public void similarity_deletion() {
    // "termination" -> "terminatio": final 'n' dropped.
    assertThat(StringSimilarity.damerauLevenshteinSimilarity("termination", "terminatio"))
        .isEqualTo(1.0 - 1.0 / 11);
  }

  @Test
  public void similarity_insertion() {
    // "overflow" -> "overfllow": extra 'l' inserted.
    assertThat(StringSimilarity.damerauLevenshteinSimilarity("overflow", "overfllow"))
        .isEqualTo(1.0 - 1.0 / 9);
  }

  @Test
  public void similarity_substitution() {
    // "datarace" -> "dataroce": 'a' replaced by 'o'.
    assertThat(StringSimilarity.damerauLevenshteinSimilarity("datarace", "dataroce"))
        .isEqualTo(1.0 - 1.0 / 8);
  }

  @Test
  public void similarity_issueExample_memsafetyTypo() {
    // The motivating example from ISSUE.md / CPAchecker's ConfigurationFileChecks.java, which
    // checks `spec.contains("memorysafety")`.
    assertThat(StringSimilarity.damerauLevenshteinSimilarity("memsafety", "memorysafety"))
        .isEqualTo(0.75);
  }

  @Test
  public void similarity_veryDifferent_isLow() {
    assertThat(StringSimilarity.damerauLevenshteinSimilarity("memorysafety", "datarace"))
        .isLessThan(0.5);
  }

  @Test
  public void similarity_bothEmpty() {
    assertThat(StringSimilarity.damerauLevenshteinSimilarity("", "")).isEqualTo(1.0);
  }

  @Test
  public void similarity_oneEmpty() {
    assertThat(StringSimilarity.damerauLevenshteinSimilarity("", "termination")).isEqualTo(0.0);
  }

  @Test
  public void similarity_nullFirstArg_throws() {
    assertThrows(
        NullPointerException.class,
        () -> StringSimilarity.damerauLevenshteinSimilarity(null, "termination"));
  }

  @Test
  public void similarity_nullSecondArg_throws() {
    assertThrows(
        NullPointerException.class,
        () -> StringSimilarity.damerauLevenshteinSimilarity("termination", null));
  }

  private static final ImmutableList<String> CPACHECKER_CONFIG_OPTION_NAMES =
      ImmutableList.of(
          "memorysafety",
          "memorycleanup",
          "overflow",
          "datarace",
          "termination",
          "witness.validation.violation",
          "witness.validation.correctness");

  @Test
  public void findClosestMatches_realTypo_memsafety() {
    assertThat(
            StringSimilarity.findClosestMatches("memsafety", CPACHECKER_CONFIG_OPTION_NAMES, 0.6))
        .containsExactly("memorysafety");
  }

  @Test
  public void findClosestMatches_realTypo_trmination() {
    assertThat(
            StringSimilarity.findClosestMatches("trmination", CPACHECKER_CONFIG_OPTION_NAMES, 0.6))
        .containsExactly("termination");
  }

  @Test
  public void findClosestMatches_thresholdIsInclusive() {
    // "terminatio" vs "termination": exactly one deletion, same pair as similarity_deletion above.
    double exactScore = 1.0 - 1.0 / 11;
    assertThat(
            StringSimilarity.findClosestMatches(
                "terminatio", ImmutableList.of("termination"), exactScore))
        .containsExactly("termination");
  }

  @Test
  public void findClosestMatches_noCandidates_returnsEmpty() {
    assertThat(StringSimilarity.findClosestMatches("termination", ImmutableList.of(), 0.5))
        .isEmpty();
  }

  @Test
  public void findClosestMatches_noneMeetThreshold_returnsEmpty() {
    assertThat(
            StringSimilarity.findClosestMatches("zzzzzzzzzz", CPACHECKER_CONFIG_OPTION_NAMES, 0.99))
        .isEmpty();
  }

  @Test
  public void findClosestMatches_multipleMatches_orderedByDescendingScore() {
    // "terminati" is "termination" minus its last 2 chars (2 deletions, score 1 - 2/11).
    // "terminatio" is "termination" minus its last char (1 deletion, score 1 - 1/11).
    // Listed in ascending-score order on input to prove the method actually sorts them.
    assertThat(
            StringSimilarity.findClosestMatches(
                "termination", ImmutableList.of("terminati", "terminatio"), 0.5))
        .containsExactly("terminatio", "terminati")
        .inOrder();
  }

  @Test
  public void findClosestMatches_ties_keepInputOrder() {
    // Both candidates are exactly one substitution away from "overflow": '"overflox"' differs at
    // the last character, "iverflow" at the first. Equal scores, so input order must be kept.
    ImmutableList<String> candidates = ImmutableList.of("overflox", "iverflow");
    assertThat(StringSimilarity.findClosestMatches("overflow", candidates, 0.5))
        .containsExactly("overflox", "iverflow")
        .inOrder();
  }

  @Test
  public void findClosestMatches_duplicateCandidates_areNotDeduplicated() {
    ImmutableList<String> candidates = ImmutableList.of("termination", "termination");
    assertThat(StringSimilarity.findClosestMatches("termination", candidates, 0.5))
        .containsExactly("termination", "termination")
        .inOrder();
  }

  @Test
  public void findClosestMatches_nullStringToCompareTo_throws() {
    assertThrows(
        NullPointerException.class,
        () -> StringSimilarity.findClosestMatches(null, CPACHECKER_CONFIG_OPTION_NAMES, 0.5));
  }

  @Test
  public void findClosestMatches_nullCandidates_throws() {
    assertThrows(
        NullPointerException.class,
        () -> StringSimilarity.findClosestMatches("termination", null, 0.5));
  }
}
