/*
 * 
 * .
 */
package com.kaibla.hamster.util;

import com.kaibla.hamster.collections.BalancedTree;
import static com.kaibla.hamster.testutils.Assertions.assertOrder;
import static java.lang.System.currentTimeMillis;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
/**
 *
 * @author kai
 */
public class BalancedTreeTest {
    
    public BalancedTreeTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

   @Test
   public void testSimpleOrdering() throws InterruptedException, Exception {
     Comparator<Integer> c = new Comparator<Integer>() {
            @Override
         public int compare(Integer t, Integer t1) {
             return t.compareTo(t1);
         }
     };
     TreeSet<Integer> reference = new TreeSet<Integer>(c);
     BalancedTree<Integer> tree = new BalancedTree<Integer>(c);
     for(int i=0; i < 11; i++) {         
         reference.add(i);
         tree.add(i);
//         LOG.info("adding entry: "+i);
     }
     
//     for(Integer i:tree) {
//         LOG.info("tree entry: "+i);
//     }
//     try {
      assertOrder(reference, tree);
//     } catch (Exception ex) {
//         new BalancedTreeBrowser(tree).setVisible(true);
//         Thread.sleep(30000);
//         throw ex;
//     }
   }
   
   @Test
   public void testRandomOrdering() throws InterruptedException, Exception {
     Comparator<Integer> c = new Comparator<Integer>() {
            @Override
         public int compare(Integer t, Integer t1) {
             return t.compareTo(t1);
         }
     };
       Random r= new Random();
     TreeSet<Integer> reference = new TreeSet<Integer>(c);
     BalancedTree<Integer> tree = new BalancedTree<Integer>(c);
     for(int i=0; i < 100; i++) { 
         Integer m= r.nextInt();
         reference.add(m);
         tree.add(m);         
     }
      assertOrder(reference, tree);
   }
   
   @Test
   public void testRandomRemove() throws InterruptedException, Exception {
     Comparator<Integer> c = new Comparator<Integer>() {
            @Override
         public int compare(Integer t, Integer t1) {
             return t.compareTo(t1);
         }
     };
     Random r= new Random();
     TreeSet<Integer> reference = new TreeSet<Integer>(c);
     BalancedTree<Integer> tree = new BalancedTree<Integer>(c);     
     HashSet<Integer> remove= new HashSet<Integer>();
     for(int i=0; i < 100; i++) { 
         Integer m= r.nextInt();
         reference.add(m);
         tree.add(m);         
         if(r.nextBoolean()) {
             remove.add(m);
         }
     }
     for(Integer m: remove) {
         tree.remove(m);
         reference.remove(m);
     }
      assertOrder(reference, tree);
   }
   
   
   
   @Test
   public void testPerformanceAdd() throws InterruptedException, Exception {
     Comparator<Integer> c = new Comparator<Integer>() {
            @Override
         public int compare(Integer t, Integer t1) {
             return t.compareTo(t1);
         }
     };
     Random r= new Random();
     TreeSet<Integer> reference = new TreeSet<Integer>(c);
     BalancedTree<Integer> tree = new BalancedTree<Integer>(c);     
     HashSet<Integer> remove= new HashSet<Integer>();
     HashSet<Integer> add= new HashSet<Integer>();
    
 
     for(int i=0; i < 500000; i++) { 
         Integer m= r.nextInt();
         add.add(m);
         if(r.nextBoolean()) {
             remove.add(m);
         }
     }
     
     LOG.log(Level.INFO, "testTimeAdd BalancedTree {0}", testTimeAdd(tree,add));
     LOG.log(Level.INFO, "testTimeAdd TreeSet {0}", testTimeAdd(reference,add));
     LOG.log(Level.INFO, "testTimeRead BalancedTree {0}", testTimeReads(tree,100));
     LOG.log(Level.INFO, "testTimeRead TreeSet {0}", testTimeReads(reference,100));
     
     LOG.log(Level.INFO, "testTimeRemove BalancedTree {0}", testTimeRemove(tree,remove));
     LOG.log(Level.INFO, "testTimeRemove TreeSet {0}", testTimeRemove(reference,remove));
     LOG.log(Level.INFO, "testTimeRead BalancedTree {0}", testTimeReads(tree,1000));
     LOG.log(Level.INFO, "testTimeRead TreeSet {0}", testTimeReads(reference,1000));
     
   }
   
   private long testTime(SortedSet s, Collection add, Collection remove) {
       long startTime = currentTimeMillis();
       for(Object a: add) {
           s.add(a);
       }
       for(Object r : remove) {
           s.remove(r);
       }
       long time= currentTimeMillis()-startTime;
       return time;
   }
   
    private long testTimeAdd(SortedSet s, Collection add) {
       long startTime = currentTimeMillis();
       for(Object a: add) {
           s.add(a);
       }      
       long time= currentTimeMillis()-startTime;
       return time;
   }
   
   private long testTimeRemove(SortedSet s,  Collection remove) {
       long startTime = currentTimeMillis();      
       for(Object r : remove) {
           s.remove(r);
       }
       long time= currentTimeMillis()-startTime;
       return time;
   }
   
   private long testTimeReads(SortedSet s,  int  repeats) {
       long startTime = currentTimeMillis();      
       for(int i = 0; i < repeats; i++) {
           Iterator iter=s.iterator();
           while(iter.hasNext()) {
               iter.next();
           }
       }
       long time= currentTimeMillis()-startTime;
       return time;
   }
    private static final Logger LOG = getLogger(BalancedTreeTest.class.getName());
   
}