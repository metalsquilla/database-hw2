# database-hw2
Implement B+ tree using Java

Author: Tiejian Zhang, Yuxiang Peng, Zhongkong Hu

## usage
```shell
git clone https://github.com/tiejian/database-hw2.git
cd ./database-hw2/
bash compile.sh
bash run.sh
```
## coding logic
### search
The most trivial idea is to search the tree recursively, so we added a helper function called "searchHelper" in charge of that. Specifically, if the key is less than the first key in the index node, then it should traverse the leftmost child, and similarly, if the key is larger than the last key in the index node, then it should go into the rightmost child. Otherwise, we should traverse through all the keys in the index node to find the leaf node. In our implementation, we used linear search as listed below, but this can be optimized using binary search since all the keys are sorted. We didn't implement it due to a lack of time.

```java
// Traverse through the index to find the leafNode
// TODO: This can be optimized using binary search
for (int i = 1; i < index.keys.size(); i++) {
  if (index.keys.get(i).compareTo(key) > 0)
    return searchHelper(index.children.get(i), key);
}
```

### insert
When the root is null, obviously we should create a new leaf node with a (key, value) entry, otherwise, we should search the leaf node to insert that entry and handle possible overflow. Therefore, we created a helper function called "insertHelper" which does the following things:

* Recursively find the leaf node to be inserted.
* If the leaf node is found, insert the given entry.
* When the current root in this function is going to be overflowed, handle it using either splitLeafNode or splitIndexNode.
* Return the new Entry object after handling overflow.

### delete
The deletion is like the inverse process of insertion, so the code structures of the two are quite similar, and we also writed a "deleteHelper" function. The logic is listed as follows:

* Recursively find the leaf node to delete the given key.
* If the leaf node is found, find the key and delete the entry.
* When the current root in this function is going to be underflowed, handle it using either handleLeafNodeUnderflow or handleIndexNodeUnderflow.
* Return the splitting index to the previous caller, so that we can keep the tree well-structured (every node is valid).

However, the node underflow is more complex to handle than overflow since either leaf node or index node could be merged or redistributed with its siblings. So we added a parent and the corresponding child index to the Node class. This makes it easier to find the siblings of a node.

## acknowledgement
1. In order to get a better understanding of implementing B+ tree, we have read the blog posted by Lin Li at http://jxlilin.blogspot.com/2013/11/b-tree-implementation-in-java.html.
2. We referred http://stackoverflow.com/ to solve some syntax problems, such as creating a new Entry using new AbstractMap.SimpleEntry<K, Node<K, T>>(key, node) and trim an ArrayList effectively using ArrayList.subList(begin, end).clear().
