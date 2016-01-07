package com.kaibla.hamster.monitoring;

import com.kaibla.hamster.base.HamsterEngine;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.lang.management.ManagementFactory.getThreadMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author kai
 */
public class DeadLockMonitor {
    
    public DeadLockMonitor(final HamsterEngine engine) {
        Thread th = new Thread(new Runnable() {
            
            @Override
            public void run() {
                while (!engine.isDestroyed()) {
                    try {
                        ThreadMXBean bean = getThreadMXBean();
                        long[] threadIds = bean.findDeadlockedThreads(); // Returns null if no threads are deadlocked. 
                        if (threadIds != null) {
                            ThreadInfo[] infos = bean.getThreadInfo(threadIds);
                            
                            for (ThreadInfo info : infos) {
                                LOG.log(Level.INFO, "found Deadlock Thread:{0}  lock: {1}  owner: {2}", new Object[]{info.
                                    getThreadName(), info.
                                    getLockName(), info.
                                    getLockOwnerName()});
                                
                                Thread deadlockedThread = findThreadById(info.
                                        getThreadId());
                                StackTraceElement[] stack = deadlockedThread.
                                        getStackTrace();;
                                for (StackTraceElement el : stack) {
                                    LOG.log(Level.INFO, "      {0}", el);
                                }
                                
                            }
                            //exit(0);
                        }
                        sleep(1000);
                        
                    } catch (InterruptedException ex) {
                        getLogger(HamsterEngine.class.
                                getName()).
                                log(Level.SEVERE, null, ex);
                    }
                    
                }
            }
        });
        th.setDaemon(true);
        th.setName("DeadLockMonitor");
        th.start();
        engine.addThread(th);
    }
    
    public static void printThreadSnapshot() {
        ThreadMXBean bean = getThreadMXBean();
        LOG.info("printThreadSnapshot");
        ThreadInfo[] infos = bean.dumpAllThreads(true, true);        
        for (ThreadInfo info : infos) {
            LOG.log(Level.INFO, "dump ThreadInfo :{0}  lock: {1}  owner: {2}", new Object[]{info.getThreadName(), info.
                getLockName(), info.getLockOwnerName()});
            
            Thread deadlockedThread = findThreadById(info.getThreadId());
            StackTraceElement[] stack = deadlockedThread.getStackTrace();;
            for (StackTraceElement el : stack) {
                LOG.log(Level.INFO, "      {0}", el);
            }            
        }
    }
    
    private static Thread findThreadById(long id) {
        ThreadGroup root = currentThread().getThreadGroup().getParent();
        return findThreadById(root, 0, id);
    }
    
    private static Thread findThreadById(ThreadGroup group, int level, long id) {
        // Get threads in `group'
        int numThreads = group.activeCount();
        Thread[] threads = new Thread[numThreads * 2];
        numThreads = group.enumerate(threads, false);

        // Enumerate each thread in `group'
        for (int i = 0; i < numThreads; i++) {
            // Get thread
            Thread thread = threads[i];
            if (thread.getId() == id) {
                return thread;
            }
        }

        // Get thread subgroups of `group'
        int numGroups = group.activeGroupCount();
        ThreadGroup[] groups = new ThreadGroup[numGroups * 2];
        numGroups = group.enumerate(groups, false);

        // Recursively visit each subgroup
        for (int i = 0; i < numGroups; i++) {
            Thread t = findThreadById(groups[i], level + 1, id);
            if (t != null) {
                return t;
            }
        }
        return null;
    }
    private static final Logger LOG = getLogger(DeadLockMonitor.class.getName());
}
