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
	public Entry<K, Node<K,T>> splitLeafNode(LeafNode<K,T> leaf, ...) {

		return null;
	}

	/**
	 * TODO split an indexNode and return the new right node and the splitting
	 * key as an Entry<slitingKey, RightNode>
	 *
	 * @param index, any other relevant data
	 * @return new key/node pair as an Entry
	 */
	public Entry<K, Node<K,T>> splitIndexNode(IndexNode<K,T> index, ...) {

		return null;
	}

	/**
	 * TODO Delete a key/value pair from this B+Tree
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

			}
		}
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
