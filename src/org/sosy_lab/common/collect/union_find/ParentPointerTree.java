// This file is part of SoSy-Lab Common,
// a library of useful utilities:
// https://github.com/sosy-lab/java-common-lib
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.common.collect.union_find;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParentPointerTree<T> {
  private final TreeNode<T> root;
  private final List<TreeNode<T>> listOfNodes;
  private final Map<T, TreeNode<T>> mapOfNodes;
  private int nextParentIndex;
  private boolean timeToMoveOn;
  private int size;

  public ParentPointerTree(T rootValue) {
    root = TreeNode.getNewRootNode(rootValue);
    listOfNodes = new ArrayList<>();
    listOfNodes.add(this.root);
    mapOfNodes = new HashMap<>();
    mapOfNodes.put(rootValue, root);
    nextParentIndex = 0;
    timeToMoveOn = false;
    size = 1;
  }

  public TreeNode<T> getRoot() {
    return root;
  }

  public int getSize() {
    return size;
  }

  public boolean contains(T value) {

    return mapOfNodes.containsKey(value);
  }

  public void addAsNewNode(T value) {

    TreeNode<T> node = TreeNode.getNewNode(listOfNodes.get(nextParentIndex), value);
    listOfNodes.add(node);
    mapOfNodes.put(value, node);
    updateNextParent();
    size++;
  }

  public boolean appendTree(ParentPointerTree<T> tree) {

    TreeNode<T> rootToBeAdded = tree.getRoot();

    TreeNode<T> parent = listOfNodes.get(nextParentIndex);
    Preconditions.checkNotNull(parent);
    rootToBeAdded.setParent(parent);

    size += tree.size;
    listOfNodes.addAll(tree.listOfNodes);
    mapOfNodes.putAll(tree.mapOfNodes);
    updateNextParent();

    return true;
  }

  public Set<T> getSetOfNodeValues() {

    return mapOfNodes.keySet();
  }

  // will currently create a binary tree as it increases counter every 2nd insert
  private void updateNextParent() {

    if (!timeToMoveOn) {
      timeToMoveOn = true;
    } else {
      nextParentIndex++;
      timeToMoveOn = false;
    }
  }
}
