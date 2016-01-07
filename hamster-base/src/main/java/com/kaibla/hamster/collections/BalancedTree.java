/*
 * BalancedTree.java
 *
 * Created on 16. August 2004, 13:45
 */
package com.kaibla.hamster.collections;

import static java.lang.System.currentTimeMillis;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author kai
 */
public class BalancedTree<T> implements SortedSet<T> {

    public Entry root = null;
    public Entry first = null;
    public Entry last = null;
    /**
     * is true if a new node was created, until the tree is balanced.
     */
    private boolean isnew = false;
    private BalancedTree tree = this;
    private int size = 0;
    private Comparator<T> comp;
    private HashMap<T, Entry> entryMap = new HashMap();

    /**
     * Creates a new instance of IntegerTree
     */
    public BalancedTree(Comparator<T> comp) {
        this.comp = comp;
    }

    public void debug() {
        boolean error = false;
        LinkedList all = new LinkedList();
        if (root == null) {
            LOG.log(Level.INFO, "debug keine Wurzel: {0}", toString());
            return;
        } else {
            debug(all, root);
        }
        //die Liste überprüfen:
        if (all.getFirst() != first) {
            LOG.info("debug first falsch gesetzt");
            error = true;
        }
        if (all.getLast() != last) {
            LOG.info("debug last falsch gesetzt");
            error = true;
        }

        Iterator iter = all.iterator();
        Entry entry = first;
        while (iter.hasNext()) {
            Entry next = (Entry) iter.next();
            if (next != entry) {
                error = true;
            }
            if (entry != null) {
                entry = entry.next;
            }
        }
        if (error) {
            LOG.log(Level.INFO, "debug: all: {0}", all);
            LOG.log(Level.INFO, "debug: toString: {0}", toString());

        }
    }

    public void debug(LinkedList all, Entry entry) {

        if (entry.left != null) {
            debug(all, entry.left);
            if (entry.left.parent != entry) {
            }
        }
        all.add(entry);
        if (entry.right != null) {
            debug(all, entry.right);
            if (entry.right.parent != entry) {
            }
        }
    }

//    /**
//     * Return the sorted content of the tree.
//     */
//    public LinkedList getValues() {
//        LinkedList list = new LinkedList();
//        if (first == null) {
//            return list;
//        }
//        Entry selected = first;
//        // LOG.info("tree:getValues() ");
//        while (selected != null) {
//            if (selected.userObject instanceof LinkedList) {
//                //     LOG.info("  tree: list at "+selected.value+" "+selected.userObject);
//                list.addAll((LinkedList) selected.userObject);
//            } else {
//                list.add(selected.userObject);
//                //     LOG.info(  "tree: object at "+selected.value+" "+selected.userObject);
//            }
//            selected = selected.next;
//        }
//        return list;
//    }
    @Override
    public String toString() {
        String result = "";
        Entry selected = first;
        // LOG.info("tree:getValues() ");
        while (selected != null) {
            result += " " + selected.userObject + " {";
            if (selected.userObject instanceof LinkedList) {
                //     LOG.info("  tree: list at "+selected.value+" "+selected.userObject);
                result += selected.userObject.toString();
            } else {
                result += selected.userObject.toString();
                //     LOG.info(  "tree: object at "+selected.value+" "+selected.userObject);
            }
            result += "};";
            selected = selected.next;
        }
        return result;
    }

    private void put(T obj) {
        Entry entry = getEntry(obj, true);
        entry.userObject = obj;
    }

    private void remove(Entry entry) {
        Entry p = entry.parent;
        while (p != null) {
            p.weight--;
            if (p == p.parent) {
                throw new IllegalStateException("Entry has itself has parent");
            }

            p = p.parent;
        }

        Entry replacement = null;
        if (entry.left != null && entry.right != null) {
            if (entry.left.weight > entry.right.weight) {
                replacement = entry.left;
                if (replacement.right == null) {
                    replacement.right = entry.right;
                    replacement.right.parent = replacement;
                    if (replacement == replacement.parent) {
                        LOG.info("parentfehler:1");
                    }
                } else {
                    //dass am weitesten rechts Liegende suchen:
                    Entry r = replacement.right;
                    while (r.right != null) {
                        r = r.right;
                    }
                    r.right = entry.right;
                    r.right.parent = r;
                    if (r.right == r.right.parent) {
                        LOG.info("parentfehler:2");
                    }
                }
            } else {
                replacement = entry.right;
                if (replacement.left == null) {
                    replacement.left = entry.left;
                    replacement.left.parent = replacement;
                    if (replacement.left == replacement.left.parent) {
                        LOG.info("parentfehler:3");
                    }
                } else {
                    //dass am weitesten rechts Liegende suchen:
                    Entry r = replacement.left;
                    while (r.left != null) {
                        r = r.left;
                    }
                    r.left = entry.left;
                    r.left.parent = r;
                    if (r.left == r.left.parent) {
                        LOG.info("parentfehler:4");
                    }
                }

            }
        } else if (entry.left != null) {
            replacement = entry.left;
        } else if (entry.right != null) {
            replacement = entry.right;
        }

        if (entry.parent != null && entry != root) {

            if (entry.parent.left == entry) {
                entry.parent.left = replacement;
                if (replacement != null) {
                    replacement.parent = entry.parent;
                    if (replacement == replacement.parent) {
                        LOG.info("parentfehler:5");
                    }
                }
            } else {
                entry.parent.right = replacement;
                if (replacement != null) {
                    replacement.parent = entry.parent;
                    if (replacement == replacement.parent) {
                        LOG.info("parentfehler:6");
                    }
                }
            }

        } else {
            root = replacement;
            if (replacement != null) {
                replacement.parent = null;
            }
        }

        //Eintrag entfernen
        if (entry == first) {
            first = entry.next;
        }
        if (entry == last) {
            last = entry.previous;
        }
        entry.parent = null;
        if (entry.previous != null) {
            entry.previous.next = entry.next;
        }
        if (entry.next != null) {
            entry.next.previous = entry.previous;
        }
        size--;
        entryMap.remove(entry.userObject);
    }

    public Entry getEntry(T value, boolean createnew) {
        if (!createnew) {
            return entryMap.get(value);
        } else {
            if (root == null) {
                if (createnew) {
                    root = new Entry(value);
                    first = root;
                    last = root;
                    return root;
                } else {
                    return null;
                }
            }

            isnew = false;
            Entry result = getEntry(value, root, createnew);
            if (createnew && isnew) {
                //
                result.balance(false);
                // root.balance();
            }
            isnew = false;
            return result;
        }
    }

    private Entry getEntry(T value, Entry entry, boolean createnew) {

        while (entry != null) {
            Entry selected = null;
            if (createnew) {
                /**
                 * A new node was created under this node, so add something to his weight.
                 */
                // entry.balance();
                //entry.weight++;
            }
            entry.usage++;
            int diff = comp.compare(value, entry.userObject);
            if (diff == 0) {
                return entry;
            } else if (diff <= -1) {
                selected = entry.left;
                if (selected == null) {
                    if (createnew) {
                        isnew = true;
                        entry.left = new Entry(value);
                        entry.addBefore(entry.left);
                        entry.left.parent = entry;

                        return entry.left;
                    } else {
                        return null;
                    }
                }
            } else if (diff >= 1) {
                selected = entry.right;
                if (selected == null) {
                    if (createnew) {
                        isnew = true;
                        entry.right = new Entry(value);
                        entry.addAfter(entry.right);
                        entry.right.parent = entry;

                        return entry.right;
                    } else {
                        return null;
                    }
                }

            } else {
                throw new IllegalStateException("Comparator is only allowed to return -1,0 or 1, but returned " + diff + "  " + comp.getClass().getCanonicalName());
            }

            entry = selected;
        }
        return entry;
    }

    @Override
    public int size() {
        return size;
    }
    public long time = 0;

    public long testtime() {
        long newtime = 0;
        newtime = currentTimeMillis();
        long result = newtime - time;
        time = newtime;
        return result;
    }

    @Override
    public Comparator<? super T> comparator() {
        return comp;
    }

    @Override
    public SortedSet<T> subSet(T e, T e1) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SortedSet<T> headSet(T e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SortedSet<T> tailSet(T e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public T first() {
        if (first == null) {
            return null;
        }
        return first.userObject;
    }

    @Override
    public T last() {
        if (last == null) {
            return null;
        }
        return last.userObject;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        return entryMap.containsKey(o);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            Entry current = first;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public T next() {
                T value = current.userObject;
                current = current.next;
                return value;
            }

            @Override
            public void remove() {
                Entry toRemove = current;
                current = current.next;
                tree.remove(toRemove);
            }
        };
    }

    @Override
    public Object[] toArray() {
        Object[] array = new Object[this.size()];
        return toArray(array);
    }

    @Override
    public <T> T[] toArray(T[] array) {
        int i = 0;
        for (Object o : this) {
            array[i] = (T) o;
            i++;
        }
        return array;
    }

    @Override
    public boolean add(T e) {
        if (entryMap.containsKey(e)) {
            return false;
        }
        put(e);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (entryMap.containsKey(o)) {
            Entry e = entryMap.get(o);
            remove(e);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean containsAll(Collection<?> clctn) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean addAll(Collection<? extends T> clctn) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean retainAll(Collection<?> clctn) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean removeAll(Collection<?> clctn) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clear() {
        entryMap.clear();
        first = null;
        last = null;
        root = null;
        size = 0;
    }

    public final class Entry {

        public Entry left = null;
        public Entry right = null;
        public Entry previous = null;
        public Entry next = null;
        Entry parent = null;
        int weight = 1;
        int usage = 1;
        public T userObject;

        public Entry() {
        }

        public Entry(T userObject) {
            this.userObject = userObject;
            entryMap.put(userObject, this);
            size++;
        }

        /**
         * Adds a Entry after this entry.
         */
        private void addAfter(Entry entry) {
            if (next != null) {
                next.previous = entry;
            } else {
                tree.last = entry;
            }
            entry.previous = this;
            entry.next = this.next;
            next = entry;
        }

        /**
         * Adds a Entry before this entry,
         */
        private void addBefore(Entry entry) {
            if (previous != null) {
                previous.next = entry;
                entry.previous = previous;
            } else {
                tree.first = entry;
            }
            entry.next = this;
            entry.previous = this.previous;
            previous = entry;
        }

        private void remove() {

            if (parent != null) {
                if (this == parent.left) {
                    parent.left = null;
                } else if (this == parent.right) {
                    parent.right = null;
                }
                parent = null;
            }
        }

        private void calcWeight() {
            //usage++;
            weight = 1;
            //    weight=1;
            if (right != null) {
                weight += right.weight;
            }
            if (left != null) {
                weight += left.weight;
            }
        }

        /**
         * Balances this node and all its parents.
         */
        private void balance(boolean onestep) {

            Entry newroot = null;
            //the Entry which is balanced
            Entry selected = this;
            while (selected != null) {

                //  if(rounds > halfsize)return;
                int leftweight = 0;
                int rightweight = 0;
                selected.weight = 1;

                if (selected.left != null) {
                    selected.weight += selected.left.weight;
                    leftweight = selected.left.weight;
                }
                if (selected.right != null) {
                    selected.weight += selected.right.weight;
                    rightweight = selected.right.weight;
                }

                if (leftweight > 2 * rightweight + 1) {
                    /**
                     * The left side too heavy.
                     */
                    //    new kai.mparser.gui.Integer1TreeBrowser(tree);
                    Entry oldroot = selected;
                    newroot = selected.left;
                    newroot.remove();

                    if (oldroot.parent != null) {

                        newroot.parent = oldroot.parent;
                        if (oldroot.parent.right == oldroot) {
                            //wenn die alte Wurzle auf der rechten Seite war
                            oldroot.parent.right = newroot;
                        } else if (oldroot.parent.left == oldroot) {
                            //wenn die alte Wurzel auf der linken Seite war
                            oldroot.parent.left = newroot;
                        }

                    }
                    Entry right = newroot.right;
                    oldroot.remove();
                    if (right != null) {
                        right.remove();
                        oldroot.left = right;
                        right.parent = oldroot;
                    }
                    newroot.right = oldroot;
                    oldroot.parent = newroot;

                    oldroot.calcWeight();
                    newroot.calcWeight();

                    if (oldroot == root) {
                        root = newroot;
                        root.parent = null;
                    }
                    selected = newroot.parent;

                } else if (rightweight > 2 * leftweight + 1) {
                    /**
                     * The right side too heavy.
                     */
                    Entry oldroot = selected;

                    newroot = selected.right;
                    newroot.remove();
                    if (oldroot.parent != null) {

                        newroot.parent = oldroot.parent;
                        if (oldroot.parent.right == oldroot) {
                            //wenn die alte Wurzle auf der rechten Seite war
                            oldroot.parent.right = newroot;
                        } else if (oldroot.parent.left == oldroot) {
                            //wenn die alte Wurzle auf der linken Seite war
                            oldroot.parent.left = newroot;
                        }

                    }
                    //Die linke Seite der neuen Wurzel
                    Entry left = newroot.left;
                    //alte Wurzle entfernen
                    oldroot.remove();
                    //Wenn die neue Wurzel eine linke Seite hat
                    if (left != null) {
                        //von der neuen Wurzel entfernen
                        left.remove();
                        //auf der rechten Seite der aleten Wurzel
                        //hinzuf�gen
                        oldroot.right = left;
                        left.parent = oldroot;
                    }
                    //alte Wurzel auf die linke Seite der neuen packen:
                    newroot.left = oldroot;
                    oldroot.parent = newroot;

                    oldroot.calcWeight();
                    newroot.calcWeight();
                    if (oldroot == root) {
                        root = newroot;
                        root.parent = null;
                    }
                    selected = newroot.parent;

                } else {
                    selected = selected.parent;
                }
                if (onestep) {
                    selected = null;
                }
            }
        }

        @Override
        public String toString() {
            return " " + userObject + " weight:" + weight;
        }
    }

    public T pollLast() {
        if (last != null) {
            T o = last.userObject;
            remove(last);
            return o;
        }
        return null;
    }
//    public static void main(String args[]) {
//        IntegerTree tree = new IntegerTree();
//
//////        tree.put(0,"test5");
//////        tree.put(3,"test3");
//////        tree.put(7,"test7");
//////        tree.put(2,"test2");
//////        tree.put(10,"test10");
//////        tree.put(11,"test11");
////        for(int i=0;i < 6;i++) {
////            tree.put(i,"test"+i);
////        }
////        LOG.info("getValues: "+tree.getValues());
////        LOG.info("B: "+tree.toStringB());
////        new kai.mparser.gui.Integer1TreeBrowser(tree);
//////        tree.remove(12);
//////        tree.remove(14);
//////        tree.remove(16);
//////        tree.remove(11);
////        LOG.info("first="+tree.first);
////        tree.remove(tree.first);
////        tree.remove(3);
////
////        new kai.mparser.gui.Integer1TreeBrowser(tree);
////        LOG.info("getValues: "+tree.getValues());
////        LOG.info("B: "+tree.toStringB());
////        LOG.info(" get (5): "+tree.get(5) );
////        tree.remove(3);
////        tree.remove(tree.first);
////        new kai.mparser.gui.Integer1TreeBrowser(tree);
////        LOG.info("getValues: "+tree.getValues());
////        LOG.info("B: "+tree.toStringB());
////        tree.testtime();
////        String test="test";
//        int size = 150;
////
//        for (int i = 0; i < size; i++) {
//            tree.put(i, "test" + i);
//            //      LOG.info("getValues: "+tree.toString());
//            //       LOG.info("B: "+tree.toStringB());
//
//        }
//        tree.debug();
//        new speech.gui.Integer1TreeBrowser(tree);
////        for(int i=0;i < size;i++) {
////      //      LOG.info("get("+i+")"+tree.get(i) );
////        }
////
//        for (int i = 0; i < size; i++) {
//            tree.remove(i);
//            if (tree.get(i) != null) {
//                // tree.remove(i);
//                LOG.info("Fehler! " + tree.get(i));
//                new speech.gui.Integer1TreeBrowser(tree);
//                tree.debug();
//            }
////            LOG.info("R getValues: "+tree.toString());
////            LOG.info("B: "+tree.toStringB());
//
//        }
//
//
////        tree.put(7,test);
////       // tree.remove(7,test);
////        LOG.info("put time: "+tree.testtime()+"ms");
////
////        //tree.root.balanceAll();
////
////        for(int i=0;i < size;i++) {
////            tree.get(i);
////        }
////        LOG.info("get time: "+tree.testtime()+"ms");
//
//        //    new kai.mparser.gui.Integer1TreeBrowser(tree);
//
//        //        for(int i=0;i < size;i++) {
//        //            tree.remove(i,test);
//        //        }
//        //        LOG.info("removetime: "+tree.testtime()+"ms");
//
////        LOG.info("balanced");
////       Object ob=tree.getBefore(size);
////       int n=size;
////
//
//
//
////       while(ob!=null) {
////
////           Object obj2=tree.get(n);
////           if(obj2 != null) {
////               LOG.info("get "+n+"  "+obj2);
////               LOG.info("get After "+n+"  "+tree.getAfter(n));
////               ob=obj2;
////           }
////           n--;
////       }
//
//
////        LOG.info("getBefore 94 "+tree.getEntryBefore(80));
////          for(int i=0;i < size;i++) {
////
////            tree.put(20,size+"multi-test"+i);
////
////        }
////        tree.getValues();
////        new kai.mparser.gui.Integer1TreeBrowser(tree);
////
////       // LOG.info(tree.getEntry(58,false)+"");
////        // System.exit(0);
////      //  tree.put(-2000,"osakdok");
////        Entry next=tree.first;
////        while(next!=null) {
////            LOG.info("       "+next);
////            next=next.next;
////        }
////        LOG.info("__________");
////        next=tree.previous;
////        while(next!=null) {
////            LOG.info("       "+next);
////            next=next.previous;
////        }
////
//
//
//    }
    private static final Logger LOG = getLogger(BalancedTree.class.getName());
}
