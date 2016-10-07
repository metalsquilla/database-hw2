import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

/**
 * BPlusTree Class
 * Assumptions: 1. No duplicate keys inserted
 * 2. Order D: D<=number of keys in a node <=2*D
 * 3. All keys are non-negative
 */
public class BPlusTree< K extends Comparable<K> , T > {
  public Node<K, T> root;
  public static final int D = 2;

  /**
   * Search the value for a specific key
   *
   * @param key
   * @return value
   */
  public T search(K key) {
    LeafNode<K, T> leaf = searchHelper(root, key);
    return (T) leaf.getValueByKey(key);
  }

  private LeafNode<K, T> searchHelper(Node<K, T> root, K key) {

    if (root == null) {
      return null;
    } else if (root.isLeafNode) {
      return (LeafNode<K, T> ) root;
    } else {
      // If node is an index node
      IndexNode<K, T> index = (IndexNode<K, T> ) root;

      // If key > last key in the node then traverse the rightmost child
      if (key.compareTo(index.keys.get(index.keys.size() - 1)) >= 0) {
        return searchHelper(index.children.get(index.children.size() - 1), key);
        // If key < first key in the node, then traverse the leftmost child
      } else if (key.compareTo(index.keys.get(0)) < 0) {
        return searchHelper(index.children.get(0), key);
      } else {
        // Traverse through the node to find the leafNode
        ListIterator<K> iter = index.keys.listIterator();
        while (iter.hasNext()) {
          if (iter.next().compareTo(key) > 0) {
            return searchHelper(index.children.get(iter.previousIndex()), key);
          }
        }
      }

    }
    return null;
  }

  /**
   * Insert a key/value pair into the BPlusTree
   *
   * @param key
   * @param value
   */
  public void insert(K key, T value) {
    if (root == null) {
      root = new LeafNode<K, T> (key, value);
    }

    Entry< K, Node<K, T>> overflow = insertHelper(root, key, value);
    if (overflow != null) {
      root = new IndexNode<K, T> (overflow.getKey(), root, overflow.getValue());
    }
  }

  private Entry< K, Node<K, T>> insertHelper(Node<K, T> root, K key, T value) {
    Entry< K, Node<K, T>> overflow = null;

    if (root.isLeafNode) {
      ((LeafNode<K, T> ) root).insertSorted(key, value);
      if (((LeafNode<K, T> ) root).isOverflowed()) {
        return splitLeafNode((LeafNode<K, T> ) root);
      }
    }
    else {
      IndexNode<K, T> index = (IndexNode<K, T> ) root;
      if (key.compareTo(index.keys.get(0)) < 0) {
        overflow = insertHelper(index.children.get(0), key, value);
      } else if (key.compareTo(index.keys.get(index.keys.size() - 1)) >= 0) {
        overflow = insertHelper(index.children.get(index.children.size() - 1), key, value);
      } else {
        ListIterator<K> iter = index.keys.listIterator();
        while (iter.hasNext()) {
          if (iter.next().compareTo(key) > 0) {
            overflow = insertHelper(index.children.get(iter.previousIndex()), key, value);
            break;
          }
        }
      }
    }

    return addIndexOverflow(root, overflow);
  }

  private Entry< K, Node<K, T>> addIndexOverflow(Node<K, T> root, Entry< K, Node<K, T>> overflow) {
    if (overflow != null && root instanceof IndexNode) {
      IndexNode<K, T> index = (IndexNode<K, T> ) root;
      // If overflow key is less than 1st key in index node, traverse left
      if (overflow.getKey().compareTo(index.keys.get(0)) < 0) {
        index.insertSorted(overflow, 0);
      } else if (overflow.getKey().compareTo(index.keys.get(index.keys.size() - 1)) > 0) {
        index.insertSorted(overflow, index.keys.size());
      } else {
        ListIterator<K> iter = index.keys.listIterator();
        while (iter.hasNext()) {
          if (iter.next().compareTo(overflow.getKey()) > 0) {
            index.insertSorted(overflow, iter.previousIndex());
            break;
          }
        }
      }

      if (index.isOverflowed())
        return splitIndexNode(index);
      else return null;
    }

    return overflow;
  }

  /**
	 * split a leaf and return the new right node and the splitting
	 * key as an Entry<slitingKey, RightNode>
	 *
	 * @param index, any other relevant data
	 * @return new key/node pair as an Entry
	 */
  public Entry< K, Node<K, T>> splitLeafNode(LeafNode<K, T> leaf) {

    // Splitting key is the first key in new right node
    K splitKey = (K) leaf.keys.get(D);

    List<K> rightKeys = new ArrayList<K> ();
    List < T > rightValues = new ArrayList < T > ();
    rightKeys.addAll(leaf.keys.subList(D, leaf.keys.size()));
    rightValues.addAll(leaf.values.subList(D, leaf.values.size()));

    LeafNode<K, T> right = new LeafNode<K, T> (rightKeys, rightValues);

    leaf.keys.subList(D, leaf.keys.size()).clear();
    leaf.values.subList(D, leaf.values.size()).clear();

    // update siblings
    if (leaf.nextLeaf != null) {
      right.nextLeaf = leaf.nextLeaf;
    }
    leaf.nextLeaf = right;

    return new AbstractMap.SimpleEntry< K, Node<K, T>> (splitKey, right);
  }

  /**
   * Splits an index and return the new right node and the splitting
   * key as an Entry<splitKey, RightNode>
   *
   * @param index, any other relevant data
   * @return new key/node pair as an Entry
   */
  public Entry< K, Node<K, T>> splitIndexNode(IndexNode<K, T> index) {
    K splitKey = (K) index.keys.get(D);

    // D+1th to last key in new right node
    List<K> rightKeys = new ArrayList<K> ();
    List<Node<K, T>> rightChildren = new ArrayList<Node<K, T>> ();
    rightKeys.addAll(index.keys.subList(D + 1, index.keys.size()));
    rightChildren.addAll(index.children.subList(D + 1, index.children.size()));

    IndexNode<K, T> right = new IndexNode<K, T> (rightKeys, rightChildren);

    index.keys.subList(D, index.keys.size()).clear();
    index.children.subList(D + 1, index.children.size()).clear();

    return new AbstractMap.SimpleEntry< K, Node<K, T>> (splitKey, right);
  }

  /**
   * Delete a key/value pair from this B+Tree
   *
   * @param key key of entry to be deleted
   */
  public void delete(K key) {

    int splitIndex = -1;
    if (root != null) {
      splitIndex = deleteHelper(key, root, null, splitIndex);
    }

    // If splitting bubbles up to the root, handle it here
    if (splitIndex != -1) {
      root.keys.remove(splitIndex);
      if (root.keys.size() == 0) {
        root = ((IndexNode<K, T> ) root).children.get(0);
      }
    }

    if (root.keys.size() == 0) {
      root = null;
    }
  }

  private int deleteHelper(K key, Node<K, T> child, IndexNode<K, T> parent, int splitIndex) {

    // Add the parent information into the node
    if (parent != null) {
      child.setParent(parent);
      child.setIndexInParent();
    }

    // If node is a leaf, delete the key value pair from it
    if (child.isLeafNode) {
      LeafNode<K, T> node = (LeafNode<K, T> ) child;
      ListIterator<K> iter = node.keys.listIterator();
      while (iter.hasNext()) {
        if (iter.next().compareTo(key) == 0) {
          node.keys.remove(key);
          node.values.remove(iter.previousIndex());
          break;
        }
      }

      // Handle leaf node underflow
      if (node.isUnderflowed() && node != root) {
        if (node.getIndexInParent() >= 1) {
          LeafNode<K, T> leftSibling = (LeafNode<K, T> ) node.getParent().children.get(node.getIndexInParent() - 1);
          return handleLeafNodeUnderflow(leftSibling, node, node.getParent());
        }
        else {
          LeafNode<K, T> rightSibling = (LeafNode<K, T> ) node.getParent().children.get(node.getIndexInParent() + 1);
          return handleLeafNodeUnderflow(node, rightSibling, node.getParent());
        }
      }

    }
    else {
      IndexNode<K, T> node = (IndexNode<K, T> ) child;

      if (key.compareTo(node.keys.get(0)) < 0) {
        splitIndex = deleteHelper(key, node.children.get(0), node, splitIndex);
      } else if (key.compareTo(node.keys.get(node.keys.size() - 1)) >= 0) {
        splitIndex = deleteHelper(key, node.children.get(node.children.size() - 1), node, splitIndex);
      } else {
        ListIterator<K> iter = node.keys.listIterator();
        while (iter.hasNext()) {
          if (iter.next().compareTo(key) > 0) {
            splitIndex = deleteHelper(key, node.children.get(iter.previousIndex()), node, splitIndex);
            break;
          }
        }
      }
    }

    // Split key deletion
    if (splitIndex != -1 && child != root) {
      child.keys.remove(splitIndex);
      if (child.isUnderflowed()) {
        if (child.getIndexInParent() >= 1) {
          IndexNode<K, T> leftSibling = (IndexNode<K, T> ) child.getParent().children.get(child.getIndexInParent() - 1);
          splitIndex = handleIndexNodeUnderflow(leftSibling, (IndexNode<K, T> ) child, child.getParent());
        } else {
          IndexNode<K, T> rightSibling = (IndexNode<K, T> ) child.getParent().children.get(child.getIndexInParent() + 1);
          splitIndex = handleIndexNodeUnderflow((IndexNode<K, T> ) child, rightSibling, child.getParent());
        }
      }
      else splitIndex = -1;
    }

    return splitIndex;
  }

  /**
   * Handle LeafNode Underflow (merge or redistribution)
   *
   * @param left
   *      : the smaller node
   * @param right
   *      : the bigger node
   * @param parent
   *      : their parent index node
   * @return the splitkey position in parent if merged so that parent can
   *     delete the splitkey later on. -1 otherwise
   */
  public int handleLeafNodeUnderflow(LeafNode<K, T> left, LeafNode<K, T> right,
    IndexNode<K, T> parent) {

    // If redistribute is possible
    if ((left.keys.size() + right.keys.size()) >= (2 * D)) {

      int childIndex = parent.children.indexOf(right);

      // Store all keys and all values from left and right nodes
      List<K> allKeys = new ArrayList<K> ();
      List < T > allValues = new ArrayList < T > ();
      allKeys.addAll(left.keys);
      allKeys.addAll(right.keys);
      allValues.addAll(left.values);
      allValues.addAll(right.values);

      // New size of left would be half of the total keys in left and right
      int leftSize = (left.keys.size() + right.keys.size()) / 2;

      // Clear all keys and values from left and right nodes
      left.keys.clear();
      right.keys.clear();
      left.values.clear();
      right.values.clear();

      // Add first half keys and values into left and rest into right
      left.keys.addAll(allKeys.subList(0, leftSize));
      left.values.addAll(allValues.subList(0, leftSize));
      right.keys.addAll(allKeys.subList(leftSize, allKeys.size()));
      right.values.addAll(allValues.subList(leftSize, allValues.size()));

      parent.keys.set(childIndex - 1, parent.children.get(childIndex).keys.get(0));
      return -1;
    }
    // If redistribute not possible, merge
    else {
      left.keys.addAll(right.keys);
      left.values.addAll(right.values);

      left.nextLeaf = right.nextLeaf;

      if (right.nextLeaf != null) {
        right.nextLeaf.previousLeaf = left;
      }

      int index = parent.children.indexOf(right) - 1;
      parent.children.remove(right);

      return index;
    }
  }

  /**
   * Handle IndexNode Underflow (merge or redistribution)
   *
   * @param left
   *      : the smaller node
   * @param right
   *      : the bigger node
   * @param parent
   *      : their parent index node
   * @return the splitkey position in parent if merged so that parent can
   *     delete the splitkey later on. -1 otherwise
   */
  public int handleIndexNodeUnderflow(IndexNode<K, T> left,
    IndexNode<K, T> right, IndexNode<K, T> parent) {

    int splitIndex = 0;

    // Find the splitting index
    for (int i = 0; i < parent.keys.size(); i++) {
      if (parent.children.get(i) == left && parent.children.get(i + 1) == right) {
        splitIndex = i;
      }
    }

    // Redistribute if possible
    if ((left.keys.size() + right.keys.size()) >= (2 * D)) {

      // All keys including the parent key
      List<K> allKeys = new ArrayList<K> ();
      List<Node<K, T>> allChildren = new ArrayList<Node<K, T>> ();
      allKeys.addAll(left.keys);
      allKeys.add(parent.keys.get(splitIndex));
      allKeys.addAll(right.keys);
      allChildren.addAll(left.children);
      allChildren.addAll(right.children);

      // Get the index of allKeys that will be the new parent key. It would be the middle key in case of odd
      // total keys. And one left of the middle for even number of keys.
      int newIndex = allKeys.size() / 2;
      if (allKeys.size() % 2 == 0) {
        newIndex = (allKeys.size() / 2) - 1;
      }

      // Add the new parent key to the splitting index.
      // Add all keys left of the new parent key into the left
      // Add all keys right of the new parent key into the right
      left.keys.clear();
      left.keys.addAll(allKeys.subList(0, newIndex));
      parent.keys.set(splitIndex, allKeys.get(newIndex));
      right.keys.clear();
      right.keys.addAll(allKeys.subList(newIndex + 1, allKeys.size()));

      // Add all the (n+1) children from 0 to n+1 to the left node
      // Add the rest of the children to the right index node
      left.children.clear();
      left.children.addAll(allChildren.subList(0, newIndex + 1));
      right.children.clear();
      right.children.addAll(allChildren.subList(newIndex + 1, allChildren.size()));

      return -1;

    } else {
      // Merge logic
      left.keys.add(parent.keys.get(splitIndex));
      left.keys.addAll(right.keys);
      left.children.addAll(right.children);

      parent.children.remove(parent.children.indexOf(right));
      return splitIndex;
    }
  }
}
