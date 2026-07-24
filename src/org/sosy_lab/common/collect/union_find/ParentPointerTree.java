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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ParentPointerTree<T> {
  private final TreeNode<T> root;
  private final List<TreeNode<T>> listOfNodes;
  private int nextParentIndex;
  private boolean timeToMoveOn;
  private int size;

  public ParentPointerTree(T rootValue) {
    this.root = TreeNode.getNewRootNode(rootValue);
    listOfNodes = new ArrayList<>();
    listOfNodes.add(this.root);
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
    for (TreeNode<T> current : listOfNodes) {
      if (current.getValue().equals(value)) {
        return true;
      }
    }

    return false;
  }

  public void addAsNewNode(T value) {
    TreeNode<T> node = TreeNode.getNewNode(listOfNodes.get(nextParentIndex), value);
    listOfNodes.add(node);
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
    updateNextParent();
    return true;
  }

  public Set<T> getSetOfNodeValues() {

    Set<T> allNodeValues = new HashSet<>();

    for (TreeNode<T> node : listOfNodes) {
      allNodeValues.add(node.getValue());
    }

    return allNodeValues;
  }

  // will currently create a binary tree as it increases counter every 2nd insert
  private void updateNextParent() {
    if (!timeToMoveOn) {
      timeToMoveOn = true;
    } else {
      nextParentIndex++;
    }
  }
}
