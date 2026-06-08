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
import java.util.Iterator;
import java.util.Set;

public class UnsortedUnionFind <T> implements UnionFind <T> {

  /*
   * CURRENT PROBLEMS:
   * - trying to keep set type as flexible as possible but finding certain things can't be implemented without deciding on type
   * - currently using HashSet
   * - considering using set of maps instead --> map each element of a disjoint set to canonical element of set (but then need to ensure duplicate elements do not occur)
   * - not sure ArrayList is ideal for canonical elements
   * - addSetOfSets might be dangerous as it relies on user providing set with the correct type of values
   * - was trying to make one class work for both sorted and unsorted (defined by type of set user provides) but it's looking like they're going to be separate and this will be unsorted
   */
  private HashSet<HashMap<T,T>> setOfMaps;
  private ArrayList canonicalElements;

  private UnsortedUnionFind() {
    setOfMaps = new HashSet<>();
    canonicalElements = new ArrayList<T>();
  }

  @Override
  public T find(T e) {
    //TODO handle edge cases

    for(HashMap<T,T> s : setOfMaps){
      if(s.containsValue(e)) {
        Set<T> keySet = s.keySet();
        if(keySet.size() >= 2) {
          //error as not all elements of set mapped to same canonical element
        } else {
          return keySet.iterator().next();
        }
      }
    }
  }

  @Override
  public void union(T e1, T e2) {
    //TODO

    Iterator<HashMap<T,T>> itti = setOfMaps.iterator();
    HashMap<T,T> map1 = null;
    HashMap<T,T> map2 = null;

    while(itti.hasNext()) {
      HashMap<T,T> current = itti.next();
      Set<T> keySet = current.keySet();
      T e;

      if(keySet.size() >= 2) {
        //error as not all elements of set mapped to same canonical element
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
      //TODO add elements of map2 to map1; map them to canonical elem. of map1
    } else {
      //TODO other way around (add map1 to map2
    }
    //TODO adjust canonical elements list
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
  public void addElementToNewSet(T e) {
    //TODO check whether new element already in structure and handle case where it is
    HashMap<T,T> newMap = new HashMap<>();
    newMap.put(e, e);

    setOfMaps.add(newMap);
    canonicalElements.add(e);
  }

  @Override
  public Set getAllSubsets() {
    return setOfMaps;
  }
}
