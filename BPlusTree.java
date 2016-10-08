import java.util.AbstractMap;
import java.util.Map.Entry;
import java.util.ArrayList;

/**
 * BPlusTree Class
 * Assumptions: 1. No duplicate keys inserted
 * 2. Order D: D<=number of keys in a index <=2*D
 * 3. All keys are non-negative
 */
public class BPlusTree<K extends Comparable<K> , T> {
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
    return (T)leaf.getValue(key);
  }

  // search the leaf recursively given a key
  private LeafNode<K, T> searchHelper(Node<K, T> root, K key) {
    if (root == null) {
      return null;
    } else if (root.isLeafNode) {
      return (LeafNode<K, T>) root;
    } else {
      IndexNode<K, T> index = (IndexNode<K, T>)root;

      // If key < first key in the index then traverse the leftmost child
      if (key.compareTo(index.keys.get(0)) < 0) {
        return searchHelper(index.children.get(0), key);
      } else if (key.compareTo(index.keys.get(index.keys.size() - 1)) >= 0) {
        return searchHelper(index.children.get(index.children.size() - 1), key);
      } else {
        // Traverse through the index to find the leafNode
        // TODO: This can be optimized using binary search
        for (int i = 1; i < index.keys.size(); i++) {
          if (index.keys.get(i).compareTo(key) > 0)
            return searchHelper(index.children.get(i), key);
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

  // insert recursively and handle overflow
  private Entry< K, Node<K, T>> insertHelper(Node<K, T> root, K key, T value) {
    Entry< K, Node<K, T>> overflow = null;

    if (root.isLeafNode) {
      ((LeafNode<K, T>) root).insertSorted(key, value);
      if (((LeafNode<K, T>) root).isOverflowed()) {
        return splitLeafNode((LeafNode<K, T>) root);
      }
    }
    else {
      IndexNode<K, T> index = (IndexNode<K, T>)root;
      if (key.compareTo(index.keys.get(0)) < 0) {
        overflow = insertHelper(index.children.get(0), key, value);
      } else if (key.compareTo(index.keys.get(index.keys.size() - 1)) >= 0) {
        overflow = insertHelper(index.children.get(index.children.size() - 1), key, value);
      } else {
        for (int i = 1; i < index.keys.size(); i++) {
          if (index.keys.get(i).compareTo(key) > 0) {
            overflow = insertHelper(index.children.get(i), key, value);
            break;
          }
        }
      }
    }

    // handle index overflow
    if (overflow != null && root instanceof IndexNode) {
      IndexNode<K, T> index = (IndexNode<K, T>) root;
      K ofkey = overflow.getKey();
      if (ofkey.compareTo(index.keys.get(0)) < 0) {
        index.insertSorted(overflow, 0);
      } else if (ofkey.compareTo(index.keys.get(index.keys.size() - 1)) > 0) {
        index.insertSorted(overflow, index.keys.size());
      } else {
        for (int i = 1; i < index.keys.size(); i++) {
          if (index.keys.get(i).compareTo(ofkey) > 0) {
            index.insertSorted(overflow, i);
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
	 * split a leaf and return the new right index and the splitting
	 * key as an Entry<slitingKey, RightNode>
	 *
	 * @param index, any other relevant data
	 * @return new key/index pair as an Entry
	 */
  public Entry< K, Node<K, T>> splitLeafNode(LeafNode<K, T> leaf) {
    K splitKey = (K)leaf.keys.get(D);

    ArrayList<K> rightKeys = new ArrayList<K>();
    ArrayList<T> rightValues = new ArrayList<T>();
    rightKeys.addAll(leaf.keys.subList(D, leaf.keys.size()));
    rightValues.addAll(leaf.values.subList(D, leaf.values.size()));

    LeafNode<K, T> right = new LeafNode<K, T>(rightKeys, rightValues);

    leaf.keys.subList(D, leaf.keys.size()).clear();
    leaf.values.subList(D, leaf.values.size()).clear();

    // update siblings
    if (leaf.nextLeaf != null) {
      leaf.nextLeaf.previousLeaf = right;
      right.nextLeaf = leaf.nextLeaf;
    }
    right.previousLeaf = leaf;
    leaf.nextLeaf = right;

    return new AbstractMap.SimpleEntry<K, Node<K, T>>(splitKey, right);
  }

  /**
   * Splits an index and return the new right index and the splitting
   * key as an Entry<splitKey, RightNode>
   *
   * @param index, any other relevant data
   * @return new key/index pair as an Entry
   */
  public Entry< K, Node<K, T>> splitIndexNode(IndexNode<K, T> index) {
    K splitKey = (K)index.keys.get(D);

    // D+1th to last key in new right index
    ArrayList<K> rightKeys = new ArrayList<K>();
    ArrayList<Node<K, T>> rightChildren = new ArrayList<Node<K, T>> ();
    rightKeys.addAll(index.keys.subList(D + 1, index.keys.size()));
    rightChildren.addAll(index.children.subList(D + 1, index.children.size()));

    IndexNode<K, T> right = new IndexNode<K, T>(rightKeys, rightChildren);

    index.keys.subList(D, index.keys.size()).clear();
    index.children.subList(D + 1, index.children.size()).clear();

    return new AbstractMap.SimpleEntry<K, Node<K, T>>(splitKey, right);
  }

  /**
   * Delete a key/value pair from this B+Tree
   *
   * @param key key of entry to be deleted
   */
  public void delete(K key) {
    if (root == null) return;

    int splitIndex = deleteHelper(key, root, null, -1);
    if (splitIndex != -1) {
      root.keys.remove(splitIndex);
      if (root.keys.isEmpty()) {
        root = ((IndexNode<K, T>) root).children.get(0);
      }
    }

    // if the new root is also empty, then the entire tree must be empty
    if (root.keys.isEmpty()) {
      root = null;
    }
  }

  private int deleteHelper(K key, Node<K, T> child, IndexNode<K, T> parent, int splitIndex) {
    if (parent != null) {
      child.setParent(parent);
    }

    // If child is a leaf, delete the key value pair from it
    if (child.isLeafNode) {
      LeafNode<K, T> leaf = (LeafNode<K, T>) child;
      for (int i = 0; i < leaf.keys.size(); i++) {
        if (leaf.keys.get(i).compareTo(key) == 0) {
          leaf.keys.remove(key);
          leaf.values.remove(i);
          break;
        }
      }

      // Handle leaf underflow
      if (leaf.isUnderflowed() && leaf != root) {
        if (leaf.getIndexInParent() == 0) {
          return handleLeafNodeUnderflow(leaf, leaf.nextLeaf, leaf.getParent());
        } else {
          return handleLeafNodeUnderflow(leaf.previousLeaf, leaf, leaf.getParent());
        }
      }

    }
    else {
      IndexNode<K, T> index = (IndexNode<K, T>)child;

      if (key.compareTo(index.keys.get(0)) < 0) {
        splitIndex = deleteHelper(key, index.children.get(0), index, splitIndex);
      } else if (key.compareTo(index.keys.get(index.keys.size() - 1)) >= 0) {
        splitIndex = deleteHelper(key, index.children.get(index.children.size() - 1), index, splitIndex);
      } else {
        for (int i = 1; i < index.keys.size(); i++) {
          if (index.keys.get(i).compareTo(key) > 0) {
            splitIndex = deleteHelper(key, index.children.get(i), index, splitIndex);
            break;
          }
        }
      }
    }

    // delete split key and handle overflow
    if (splitIndex != -1 && child != root) {
      child.keys.remove(splitIndex);
      if (child.isUnderflowed()) {
        if (child.getIndexInParent() == 0) {
          IndexNode<K, T> rightSibling = (IndexNode<K, T>) child.getParent().children.get(child.getIndexInParent() + 1);
          splitIndex = handleIndexNodeUnderflow((IndexNode<K, T>) child, rightSibling, child.getParent());
        } else {
          IndexNode<K, T> leftSibling = (IndexNode<K, T>) child.getParent().children.get(child.getIndexInParent() - 1);
          splitIndex = handleIndexNodeUnderflow(leftSibling, (IndexNode<K, T>) child, child.getParent());
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
   *      : the smaller index
   * @param right
   *      : the bigger index
   * @param parent
   *      : their parent index index
   * @return the splitkey position in parent if merged so that parent can
   *     delete the splitkey later on. -1 otherwise
   */
  public int handleLeafNodeUnderflow(LeafNode<K, T> left, LeafNode<K, T> right,
    IndexNode<K, T> parent) {

    // If redistributable
    int totalSize = left.keys.size() + right.keys.size();
    if (totalSize >= 2*D) {

      int childIndex = parent.children.indexOf(right);

      // Store all keys and values from left to right
      ArrayList<K> keys = new ArrayList<K>();
      ArrayList<T> vals = new ArrayList <T>();
      keys.addAll(left.keys);
      keys.addAll(right.keys);
      vals.addAll(left.values);
      vals.addAll(right.values);

      int leftSize = totalSize / 2;

      left.keys.clear();
      right.keys.clear();
      left.values.clear();
      right.values.clear();

      // Add first half keys and values into left and rest into right
      left.keys.addAll(keys.subList(0, leftSize));
      left.values.addAll(vals.subList(0, leftSize));
      right.keys.addAll(keys.subList(leftSize, keys.size()));
      right.values.addAll(vals.subList(leftSize, vals.size()));

      parent.keys.set(childIndex - 1, parent.children.get(childIndex).keys.get(0));
      return -1;
    }
    else {
      // remove right child
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
   *      : the smaller index
   * @param right
   *      : the bigger index
   * @param parent
   *      : their parent index index
   * @return the splitkey position in parent if merged so that parent can
   *     delete the splitkey later on. -1 otherwise
   */
  public int handleIndexNodeUnderflow(IndexNode<K, T> left,
    IndexNode<K, T> right, IndexNode<K, T> parent) {

    int splitIndex = -1;
    for (int i = 0; i < parent.keys.size(); i++) {
      if (parent.children.get(i) == left && parent.children.get(i + 1) == right) {
        splitIndex = i;
        break;
      }
    }

    // Redistribute if possible
    if ((left.keys.size() + right.keys.size()) >= (2 * D)) {
 - 1
      // All keys including the parent key
      ArrayList<K> keys = new ArrayList<K>();
      ArrayList<Node<K, T>> children = new ArrayList<Node<K, T>>();
      keys.addAll(left.keys);
      keys.add(parent.keys.get(splitIndex));
      keys.addAll(right.keys);
      children.addAll(left.children);
      children.addAll(right.children);

      // Get the index of the new parent key
      int newIndex = keys.size() / 2;
      if (keys.size() % 2 == 0) {
        newIndex -= 1;
      }
      parent.keys.set(splitIndex, keys.get(newIndex));

      left.keys.clear();
      right.keys.clear();
      left.children.clear();
      right.children.clear();

      left.keys.addAll(keys.subList(0, newIndex));
      right.keys.addAll(keys.subList(newIndex + 1, keys.size()));
      left.children.addAll(children.subList(0, newIndex + 1));
      right.children.addAll(children.subList(newIndex + 1, children.size()));

      return -1;
    }
    else {
      left.keys.add(parent.keys.get(splitIndex));
      left.keys.addAll(right.keys);
      left.children.addAll(right.children);

      parent.children.remove(parent.children.indexOf(right));
      return splitIndex;
    }
  }
}
