// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class UnsortedUnionFind <T> implements UnionFind <T> {

  /*
   * CURRENT PROBLEMS:
   * - trying to keep set type as flexible as possible but finding certain things can't be implemented without deciding on type
   * - set of maps --> map each element of a disjoint set to canonical element of set (but then need to ensure duplicate elements do not occur)
   * - not sure ArrayList is ideal for canonical elements
   * - addSetOfSets might be dangerous as it relies on user providing set with the correct type of values
   * - was trying to make one class work for both sorted and unsorted (defined by type of set user provides) but it's looking like they're going to be separate and this will be unsorted
   */
  private HashSet<HashMap<T,T>> setOfMaps;
  private ArrayList<T> canonicalElements; //TODO remove if continues to be unnecessary

  private UnsortedUnionFind() {
    setOfMaps = new HashSet<>();
    canonicalElements = new ArrayList<>();
  }

  @Override
  public T find(T e) {
    //TODO handle edge cases

    for(HashMap<T,T> s : setOfMaps){
      if(s.containsValue(e)) {
        Set<T> keySet = s.keySet();
        if(keySet.size() >= 2) {
          //TODO error as not all elements of set mapped to same canonical element
        } else {
          return keySet.iterator().next();
        }
      }
    }
  }

  //currently only union by size supported for unsorted union-find
  @Override
  public void union(T e1, T e2) {
    HashMap<T,T> map1 = null;
    HashMap<T,T> map2 = null;

    for(HashMap<T,T> current : setOfMaps) {
      Set<T> keySet = current.keySet();
      T e;

      if(keySet.size() >= 2) {
        //TODO error as not all elements of set mapped to same canonical element
      } else {
        e = keySet.iterator().next();

        if(e.equals(e1)) {
          map1 = current;
        } else if(e.equals(e2)) {
          map2 = current;
        }
      }

      if(map1!=null && map2!=null) {
        break;
      }
    }
    if(map1.size() > map2.size()) {
      for(T e : map2.values()) {
        map1.put(e1, e);
      }
      canonicalElements.remove(e2);
    } else {
      for(T e : map1.values()) {
        map2.put(e2, e);
      }
      canonicalElements.remove(e1);
    }
  }

  @Override
  public UnionFind<T> getEmptyInstanceOf() {
    return new UnsortedUnionFind<T>();
  }

  @Override
  public void addSetOfSets(Set set) {
    //TODO currently laid out for set of sets instead of set of maps
    //problem: extracting canonical elements to add to canonicalElements --> could call keySet() on each Map
  }

  @Override
  public void addElementToNewSet(T e) throws IllegalArgumentException {

      if (contains(e)) {
        //throw exception: element already contained
        throw new IllegalArgumentException("Element already exists");
      }

    HashMap<T,T> newMap = new HashMap<>();
    newMap.put(e, e);

    setOfMaps.add(newMap);
    canonicalElements.add(e);
  }

  @Override
  public Set getAllSubsets() {
    return setOfMaps;
  }

  private boolean contains(T e) {
    for(HashMap<T,T> current : setOfMaps) {
      if(current.containsValue(e)) {
        return true;
      }
    }
    return false;
  }
}
