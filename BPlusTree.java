import java.util.AbstractMap;
import java.util.Map.Entry;

/**
 * BPlusTree Class Assumptions: 1. No duplicate keys inserted 2. Order D:
 * D<=number of keys in a node <=2*D 3. All keys are non-negative
 * TODO: Rename to BPlusTree
 */
public class BPlusTree<K extends Comparable<K>, T> {

	public Node<K,T> root;
	public static final int D = 2;

	/**
	 * TODO Search the value for a specific key
	 *
	 * @param key
	 * @return value
	 */
	public T search(K key) {
		return null;
	}

	/**
	 * TODO Insert a key/value pair into the BPlusTree
	 *
	 * @param key
	 * @param value
	 */
	public void insert(K key, T value) {

	}

	/**
	 * TODO Split a leaf node and return the new right node and the splitting
	 * key as an Entry<slitingKey, RightNode>
	 *
	 * @param leaf, any other relevant data
	 * @return the key/node pair as an Entry
	 */
	public Entry<K, Node<K,T>> splitLeafNode(LeafNode<K,T> leaf) {
    K splittingKey = (K)leaf.keys.get(D);

    List<K> rightKeys = new ArrayList<K>();
		List<T> rightValues = new ArrayList<T>();
    rightKeys.addAll(leaf.keys.subList(D, leaf.keys.size()));
    rightValues.addAll(leaf.values.subList(D, leaf.values.size()));

    LeafNode<K, T> right = new LeafNode<K, T>(rightKeys, rightValues);

    leaf.keys.subList(D, leaf.keys.size()).clear();
    leaf.values.subList(D, leaf.values.size()).clear();

		// update next leaf
    if (leaf.nextLeaf != null) {
      right.nextLeaf = leaf.nextLeaf;
    }
    leaf.nextLeaf = right;

		return new AbstractMap.SimpleEntry<K, Node<K,T>>(splittingKey, right);
	}

	/**
	 * split an indexNode and return the new right node and the splitting
	 * key as an Entry<slitingKey, RightNode>
	 *
	 * @param index, any other relevant data
	 * @return new key/node pair as an Entry
	 */
	public Entry<K, Node<K,T>> splitIndexNode(IndexNode<K,T> index) {
		// Splitting key is the Dth key in new left node
    K splittingKey = (K)index.keys.get(D);

    // Move D+1th to the last to new right node
    List<K> rightKeys = new ArrayList<K>();
		List<Node<K, T>> rightChildren = new ArrayList<Node<K, T>>();
    rightKeys.addAll(index.keys.subList(D + 1, index.keys.size()));
    rightChildren.addAll(index.children.subList(D + 1, index.children.size()));

    IndexNode<K, T> right = new IndexNode<K, T>(rightKeys, rightChildren);

    // Remove all keys and children after Dth entry
    index.keys.subList(D, index.keys.size()).clear();
    index.children.subList(D+1, index.children.size()).clear();

		// use this to create a new entry
		return new AbstractMap.SimpleEntry<K, Node<K,T>>(splittingKey, right);
	}

	/**
	 * Delete a key/value pair from this B+Tree
	 * @param key
	 */
	public void delete(K key) {
		if (root == null) return;

		int splitIndex = -1;
		splitIndex = delteHelper(key, root, null, splitIndex);
		if (splitIndex != -1) {
			root.keys.remove(splitIndex);
			if (root.keys.isEmpty()) {
				Node<K, T> child = root.children.get(0);
				root = (IndexNode<K, T>)child;
			}
		}
		if (root.keys.isEmpty()) root = null;
	}

	// find the key value pair recursively and handle Underflows
	private int delteHelper(K key, Node<K, T> child, IndexNode<K, T> parent, int splitIndex) {
		if (parent != null) {
			child.setParent(parent);
		}

		if (child.isLeafNode()) {
			LeafNode<K, T> leaf = (LeafNode<K, T>)child;
			for (int i = 0; i < leaf.keys.size(); i++) {
				if (leaf.keys.get(i).compareTo(key) == 0) {
					leaf.keys.remove(i);
					leaf.values.remove(i - 1);
					break;
				}
			}
			// handle leaf node overflow
			if (leaf.isUnderflowed()) {
				// leaf has left sibling
				if (leaf.getIndexInParent() >= 1) {
					LeafNode<K, T> leftSibling = (LeafNode<K, T>)leaf.getParent().children.get(leaf.getIndexInParent() - 1);
    			return handleLeafNodeUnderflow(leftSibling, leaf, leaf.getParent());
				}
				else {	// has no left sibling, try right sibling instead
					LeafNode<K, T> rightSibling = (LeafNode<K, T>)leaf.getParent().children.get(leaf.getIndexInParent() + 1);
    			return handleLeafNodeUnderflow(leaf, rightSibling, leaf.getParent());
				}
			}
		}
		// the child is an index node
		else {
			IndexNode<K, T> Index = (IndexNode<K, T>)child;
			// If key is smaller than the first key in the index node, traverse left child
			if (key.compareTo(index.keys.get(0)) < 0) {
				splitIndex = deleteHelper(key, index.children.get(0), index, splitIndex);
			}
			// If key is larger than the last key in the index node, traverse right child
			else if (key.compareTo(index.keys.get(index.keys.size() - 1)) >= 0) {
				splitIndex = deleteHelper(key, index.children.get(index.keys.size() - 1), index, splitIndex);
			}
			else {
				ListIterator<K> iterator = index.keys.listIterator();
        while (iterator.hasNext()) {
          if (iterator.next().compareTo(key) > 0) {
          	splitIndex = deleteHelper(key, index.children.get(iterator.previousIndex()), index, splitIndex);
          	break;
          }
        }
			}
		}

		// delete split key
		if (splitIndex != -1 && child != root) {
			child.keys.remove(splitIndex);
			// Check child underflowed, call handle index underflow
			if (child.isUnderflowed()) {
				if (child.getIndexInParent() >= 1) {
					IndexNode<K, T> leftSibling = (IndexNode<K, T>) child.getParent().children.get(child.getIndexInParent() - 1);
					splitIndex = handleIndexNodeUnderflow(leftSibling, (IndexNode<K, T>)child, child.getParent());
				} else {
					IndexNode<K, T> rightSibling = (IndexNode<K, T>) child.getParent().children.get(child.getIndexInParent() + 1);
					splitIndex = handleIndexNodeUnderflow((IndexNode<K, T>)child, rightSibling, child.getParent());
				}
			}
			splitIndex = -1;
		}

		return splitIndex;
	}

	/**
	 * TODO Handle LeafNode Underflow (merge or redistribution)
	 *
	 * @param left
	 *            : the smaller node
	 * @param right
	 *            : the bigger node
	 * @param parent
	 *            : their parent index node
	 * @return the splitkey position in parent if merged so that parent can
	 *         delete the splitkey later on. -1 otherwise
	 */
	public int handleLeafNodeUnderflow(LeafNode<K,T> left, LeafNode<K,T> right, IndexNode<K,T> parent) {
		int totalSize = left.keys.size() + right.keys.size();
		// redistributable
		if (totalSize >= 2*D) {
			int idx = parent.children.indexOf(right);

			ArrayList<K> keys = new ArrayList<K>();
			ArrayList<T> vals = new ArrayList<T>();
			keys.addAll(left.keys);
			keys.addAll(right.keys);
			vals.addAll(left.values);
			vals.addAll(right.values);
			left.keys.clear();
			right.keys.clear();
			left.values.clear();
			right.values.clear();

			int leftSize = totalSize / 2;
			left.keys.addAll(keys.subList(0, leftSize));
			left.values.addAll(vals.subList(0, leftSize));
			right.keys.addAll(keys.subList(leftSize, totalSize));
			right.values.addAll(vals.subList(leftSize, totalSize));

			// update the starting key of the right index node
			parent.keys.set(idx - 1, right.keys.get(0));
			return -1;
		}
		else {	// merge
			left.keys.addAll(right.keys);
			left.values.addAll(right.values);

			left.nextLeaf = right.nextLeaf;
			if (right.nextLeaf != null) {
				right.nextLeaf.previousLeaf = left;
			}

			int idx = parent.children.indexOf(right) - 1;
			parent.remove(right);
			return idx;
		}
	}

	/**
	 * TODO Handle IndexNode Underflow (merge or redistribution)
	 *
	 * @param left
	 *            : the smaller node
	 * @param right
	 *            : the bigger node
	 * @param parent
	 *            : their parent index node
	 * @return the splitkey position in parent if merged so that parent can
	 *         delete the splitkey later on. -1 otherwise
	 */
	public int handleIndexNodeUnderflow(IndexNode<K,T> left, IndexNode<K,T> right, IndexNode<K,T> parent) {
		// find the position of the left node
		int pos = -1;
		for (int i = 0; i < parent.keys.size() - 1; i++) {
			if (parent.children.get(i) == left && parent.children.get(i + 1) == right) {
				pos = i;
				break;
			}
		}
		int parentKey = parent.keys.get(pos);

		int totalSize = left.keys.size() + right.keys.size();
		// redistributable
		if (totalSize >= 2*D) {
			ArrayList<K> keys = new ArrayList<K>();
			keys.addAll(left.keys);
			keys.add(parentKey);
			keys.addAll(right.keys);
			left.keys.clear();
			right.keys.clear();

			ArrayList<Node<K, T>> nodes = new ArrayList<Node<K, T>>();
			nodes.addAll(left.children);
			nodes.addAll(right.children);
			left.children.clear();
			right.children.clear();

			int newPos = totalSize / 2;
			if (totalSize % 2 = 0) newPos -= 1;
			parent.keys.set(pos, keys.get(newPos));

			left.keys.addAll(keys.subList(0, newPos));
			right.keys.addAll(keys.subList(newPos + 1, totalSize));
			left.children.addAll(nodes.subList(0, newPos));
			right.children.addAll(nodes.subList(newPos + 1, totalSize));

			return -1;
		}
		else {	// merge
			left.keys.add(parentKey);
			left.keys.addAll(right.keys);
			left.children.addAll(right.children);
			// remove the right node
			int idx = parent.children.indexOf(right);
			parent.children.remove(idx);
			return pos;
		}
	}

}
