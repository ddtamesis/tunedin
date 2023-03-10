package kdtree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Class to store KDTreeNodes in a k-d tree, a special form of a binary tree built for the quick
 * search of points in a k-dimensional space.
 *
 * @param <T> data to be stored in the KdTree, must extend KdTreeNode
 */
public class KdTree<T extends KdTreeNode> {
  private T head;
  private KdTree<T> left;
  private KdTree<T> right;
  private int axis;
  private List<T> kdTreeNodes;

  /**
   * Recursively constructs a k-d tree of the given nodes.
   *
   * @param kdTreeNodes List of nodes to construct the k-d tree with.
   * @param depth Current depth of the tree, which determines the axis to sort by.
   */
  public KdTree(List<T> kdTreeNodes, int depth) {
    int size = kdTreeNodes.size();
    this.kdTreeNodes = kdTreeNodes;
    if (size == 0) {
      // base case
      this.head = null;
      this.left = null;
      this.right = null;
    } else {
      int dimension = kdTreeNodes.get(0).getDimension();
      int currentAxis = depth % dimension;
      this.axis = currentAxis;

      // sort the list of nodes by the current relevant axis
      kdTreeNodes.sort(new AxisSorter(currentAxis));

      // find the median index to split the list at
      int medianIndex;
      if (size % 2 == 1) {
        medianIndex = (size - 1) / 2;
      } else {
        medianIndex = size / 2;
      }
      // recursively construct the rest of the tree with each half of the node list
      this.head = kdTreeNodes.get(medianIndex);
      this.left = new KdTree<>(new ArrayList<>(kdTreeNodes.subList(0, medianIndex)), depth + 1);
      this.right =
          new KdTree<>(new ArrayList<>(kdTreeNodes.subList(medianIndex + 1, size)), depth + 1);
    }
  }

  /**
   * Performs either a radius search or a k-nearest neighbors search using a specific k-d tree
   * traversal algorithm.
   *
   * @param type The type of search to perform. Can either be "neighbors" or "radius."
   * @param val If type is "neighbors", the number of neighbors to return. If type is "radius", the
   *     radius around targetPoint to search for nodes.
   * @param targetPoint The point to search around for neighbors or points in a given radius.
   * @param distanceSorter Comparator to sort the returned PriorityQueue in descending order by
   *     distance from targetPoint.
   * @param ignore HashSet of KDTreeNodes to skip when returning nodes. Used to avoid returning
   *     "name" when command format is 'neighbors k "name"' OR 'radius r "name"'.
   * @return PriorityQueue in descending order by distance from targetPoint of nearest
   *     neighbors/nodes within radius.
   */
  public PriorityQueue<T> kdTreeSearch(
      String type,
      double val,
      KdTreeNode targetPoint,
      Comparator<KdTreeNode> distanceSorter,
      HashSet<T> ignore) {
    // k is used for "neighbors" search
    int k = (int) Math.round(val);
    PriorityQueue<T> returnNodes = new PriorityQueue<>();

    if (val == 0 && type.equals("neighbors") || this.head == null) {
      // base case
      return returnNodes;
    } else {
      if ("radius".equals(type)) {
        // init priority queue with enough room for all nodes
        returnNodes = new PriorityQueue<>(this.kdTreeNodes.size(), distanceSorter);
      } else if ("neighbors".equals(type)) {
        // init priority queue with enough room for k neighbors (k + 1 in case k == 0)
        returnNodes = new PriorityQueue<>(k + 1, distanceSorter);
      }
      // recur
      kdTreeSearchRecursive(type, val, returnNodes, targetPoint, ignore);
    }
    return returnNodes;
  }

  /**
   * Helper function to recursively to perform either a radius search or a k-nearest neighbors
   * search.
   *
   * @param type The type of search to perform. Can either be "neighbors" or "radius."
   * @param val If type is "neighbors", the number of neighbors to return. If type is "radius", the
   *     radius around targetPoint to search for nodes.
   * @param returnNodes PriorityQueue in descending order by distance from targetPoint of nearest
   *     neighbors/nodes within radius.
   * @param targetPoint The point to search around for neighbors or points in a given radius.
   * @param ignore HashSet of KDTreeNodes to skip when returning nodes. Used to avoid returning
   *     "name" when command format is 'neighbors k "name"' OR 'radius r "name"'.
   */
  private void kdTreeSearchRecursive(
      String type,
      double val,
      PriorityQueue<T> returnNodes,
      KdTreeNode targetPoint,
      HashSet<T> ignore) {
    if (this.getHead() != null) {
      T currentNode = this.getHead();
      double currentDistance = currentNode.euclideanDistance(targetPoint);
      // comparison value used for traversal (differs for radius and neighbors)
      double traversalComparison = 0;
      int relevantAxis = this.getAxis();
      double relevantAxisDistance =
          targetPoint.getPoint()[relevantAxis] - currentNode.getPoint()[relevantAxis];

      if ("neighbors".equals(type)) {
        int k = (int) Math.round(val);
        if (returnNodes.size() == 0) {
          // if returnNodes is empty and ignore does not have current node, add current node
          if (!ignore.contains(currentNode)) {
            returnNodes.add(currentNode);
          }
          // this ensures recursion on subtrees
          traversalComparison = relevantAxisDistance + 1;
        } else if (returnNodes.size() < k) {
          // if returnNodes isn't full and ignore does not have current node, add current node
          if (!ignore.contains(currentNode)) {
            returnNodes.add(currentNode);
          }
          traversalComparison = returnNodes.peek().euclideanDistance(targetPoint);
        } else if (returnNodes.size() == k) {
          // if returnNodes is full, compare with farthest node & update accordingly
          traversalComparison = returnNodes.peek().euclideanDistance(targetPoint);
          if (!ignore.contains(currentNode)
              && Double.compare(currentDistance, traversalComparison) == -1) {
            returnNodes.poll();
            returnNodes.add(currentNode);
          } else {
            traversalComparison = returnNodes.peek().euclideanDistance(targetPoint);
          }
        }

      } else if ("radius".equals(type)) {
        traversalComparison = val;
        if (!ignore.contains(currentNode) && Double.compare(currentDistance, val) <= 0) {
          // if the current node's distance is within the radius, add
          returnNodes.add(currentNode);
        }
      }

      if (Double.compare(traversalComparison, relevantAxisDistance) >= 0) {
        // recur on both subtrees if traversalComparison > relevant axis dist.
        this.left.kdTreeSearchRecursive(type, val, returnNodes, targetPoint, ignore);
        this.right.kdTreeSearchRecursive(type, val, returnNodes, targetPoint, ignore);
      } else {
        double currentNodeRelevantAxis = currentNode.getPoint()[relevantAxis];
        double targetPointRelevantAxis = targetPoint.getPoint()[relevantAxis];

        if (Double.compare(currentNodeRelevantAxis, targetPointRelevantAxis) == -1) {
          // if currentNode < targetPoint on relevant axis, recur on right subtree
          this.right.kdTreeSearchRecursive(type, val, returnNodes, targetPoint, ignore);
        } else if (Double.compare(currentNodeRelevantAxis, targetPointRelevantAxis) == 1) {
          // if currentNode > targetPoint on relevant axis, recur on left subtree
          this.left.kdTreeSearchRecursive(type, val, returnNodes, targetPoint, ignore);
        } else {
          // if currentNode == targetPoint on relevant axis, recur on both subtrees
          this.left.kdTreeSearchRecursive(type, val, returnNodes, targetPoint, ignore);
          this.right.kdTreeSearchRecursive(type, val, returnNodes, targetPoint, ignore);
        }
      }
    }
  }

  /**
   * Performs either a radius search or a k-nearest neighbors search using a naive, inefficient
   * algorithm.
   *
   * @param type The type of search to perform. Can either be "neighbors" or "radius."
   * @param val If type is "neighbors", the number of neighbors to return. If type is "radius", the
   *     radius around targetPoint to search for nodes.
   * @param targetPoint The point to search around for neighbors or points in a given radius.
   * @param ignore HashSet of KDTreeNodes to skip when returning nodes. Used to avoid returning
   *     "name" when command format is 'neighbors k "name"' OR 'radius r "name"'.
   * @return PriorityQueue in descending order by distance from targetPoint of nearest
   *     neighbors/nodes within radius.
   */
  public PriorityQueue<T> naiveSearch(
      String type, double val, KdTreeNode targetPoint, HashSet<T> ignore) {
    List<T> nodeList = this.kdTreeNodes;
    int k = (int) Math.round(val);
    PriorityQueue<T> returnNodes = new PriorityQueue<>(k + 1, new DistanceSorter(targetPoint));
    if (type.equals("radius")) {
      // iterate through node list and add node if it is within the radius
      for (T node : nodeList) {
        double currentDistance = targetPoint.euclideanDistance(node);
        if (!ignore.contains(node) && Double.compare(currentDistance, val) <= 0) {
          returnNodes.add(node);
        }
      }

    } else if (type.equals("neighbors")) {
      if (k == 0 || nodeList.isEmpty()) {
        // base case
        return returnNodes;
      } else {
        for (T node : nodeList) {
          // iterate through node list and update returnNodes accordingly
          if (!ignore.contains(node)) {
            double currentDistance = targetPoint.euclideanDistance(node);
            if (returnNodes.size() == k
                && Double.compare(
                        currentDistance, returnNodes.peek().euclideanDistance(targetPoint))
                    <= 0) {
              returnNodes.poll();
              returnNodes.add(node);
            } else if (returnNodes.size() < k) {
              returnNodes.add(node);
            }
          }
        }
      }
    }
    return returnNodes;
  }

  /**
   * Getter to return head of KdTree.
   *
   * @return Head of type T of KdTree.
   */
  public T getHead() {
    return this.head;
  }

  /**
   * Getter to return left child of current KdTree.
   *
   * @return KdTree of type T
   */
  public KdTree<T> getLeft() {
    return this.left;
  }

  /**
   * Getter to return right child of current KdTree.
   *
   * @return KdTree of type T
   */
  public KdTree<T> getRight() {
    return this.right;
  }

  /**
   * Getter to return relevant axis of current KdTree.
   *
   * @return integer representing current relevant axis
   */
  public int getAxis() {
    return this.axis;
  }

  /**
   * Getter to return ArrayList of all KDTreeNodes in the tree.
   *
   * @return ArrayList of all KDTreeNodes in the tree.
   */
  public List<T> getKdTreeNodes() {
    return this.kdTreeNodes;
  }

  /**
   * Deletes the given node from the tree.
   *
   * @param nodeToDelete - the node to delete
   * @return - the new tree after deletion
   */
  public KdTree<T> deleteNode(T nodeToDelete) {
    this.kdTreeNodes.remove(nodeToDelete);
    return this.deleteNodeRecursive(nodeToDelete, 0);
  }

  /**
   * Deletes the given node from the tree recursively.
   *
   * @param nodeToDelete - the node to delete
   * @param depth - the current depth from the original root
   * @return - the current tree after deletion
   */
  private KdTree<T> deleteNodeRecursive(KdTreeNode nodeToDelete, int depth) {
    if (this.getHead() == null) {
      return this;
    }
    int totalDimension = this.getHead().getDimension();
    int currentDimension = depth % totalDimension;

    // if point to delete is present at this root
    if (this.getHead().equals(nodeToDelete)) {
      if (this.getRight().getHead() != null) {
        // find minimum in right subtree to get 'inorder successor' to replace position of the node
        // being deleted
        KdTree<T> minNode = this.right.findMinNode(currentDimension);
        this.head = minNode.getHead();
        this.right = this.right.deleteNodeRecursive(minNode.getHead(), depth + 1);
      } else if (this.getLeft().getHead() != null) {
        KdTree<T> minNode = this.left.findMinNode(currentDimension);
        this.head = minNode.getHead();
        this.right = this.left.deleteNodeRecursive(minNode.getHead(), depth + 1);
        // added the line below bc test with only left child failed
        // (7,0) replaced the deleted (6,4) head, but it was still present at the left field as well
        // so (7,0) was in the tree twice
        this.left = new KdTree<>(new ArrayList<>(), depth + 1);
      } else {
        return new KdTree<>(new ArrayList<>(), depth + 1);
      }
      return this;
    }
    // point to delete is NOT present at root
    if (nodeToDelete.getPoint()[currentDimension] < this.getHead().getPoint()[currentDimension]) {
      this.left = this.left.deleteNodeRecursive(nodeToDelete, depth + 1);
    } else {
      this.right = this.right.deleteNodeRecursive(nodeToDelete, depth + 1);
    }
    return this;
  }

  /**
   * Finds the node with the minimum value of the indicated axis
   *
   * @param axis - the axis to compare on
   * @return the node with the minimum value
   */
  public KdTree<T> findMinNode(int axis) {
    return this.findMinimumRecursive(axis, 0);
  }

  /**
   * Finds the node with the minimum value of the indicated axis recursively.
   *
   * @param axis - the axis to compare on
   * @param depth - the current depth of the tree from the original root
   * @return - the node with the minimum value
   */
  private KdTree<T> findMinimumRecursive(int axis, int depth) {
    // base case
    if (this.getHead() == null) {
      return null;
    }
    int totalDimension = this.getHead().getDimension();
    int currentDimension = depth % totalDimension;

    // compare values of axis easily if it equals currentDimension
    if (currentDimension == axis) {
      if (this.left.getHead() == null) {
        return this;
      }
      return this.min(this, this.left.findMinimumRecursive(axis, depth + 1), null, axis);
    }
    // otherwise, a better minimum may be anywhere down the tree
    // find the minimum of the given axis from here onward
    return this.min(
        this,
        this.left.findMinimumRecursive(axis, depth + 1),
        this.right.findMinimumRecursive(axis, depth + 1),
        axis);
  }

  /**
   * Compare three nodes on a given axis and return the node with the minimum value
   *
   * @param node1 - the first node to compare
   * @param node2 - the second node to compare
   * @param node3 - the third node to compare
   * @param axis - the axis to compare on
   * @return the minimum node
   */
  private KdTree<T> min(KdTree<T> node1, KdTree<T> node2, KdTree<T> node3, int axis) {
    KdTree<T> minNode = node1;
    if (node2 != null && node2.getHead().getPoint()[axis] < minNode.getHead().getPoint()[axis]) {
      minNode = node2;
    }
    if (node3 != null && node3.getHead().getPoint()[axis] < minNode.getHead().getPoint()[axis]) {
      minNode = node3;
    }
    return minNode;
  }

  @Override
  public String toString() {
    if (this.head == null) {
      return "null";
    }
    return "KdTree{"
        + "head="
        + head.toString()
        + ", left="
        + left.toString()
        + ", right="
        + right.toString()
        + ", axis="
        + axis
        + ", kdTreeNodes="
        + kdTreeNodes.toString()
        + '}';
  }
}
