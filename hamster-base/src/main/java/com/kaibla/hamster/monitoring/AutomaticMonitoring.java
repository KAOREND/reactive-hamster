package com.kaibla.hamster.monitoring;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;

/**
 *
 * @author Kai Orend
 */
public class AutomaticMonitoring {

    private static final ConcurrentHashMap<Thread, MonitoredThread> monitoredThreads = new ConcurrentHashMap<>();

    private static Thread monitorThread;

    public synchronized static Thread startMonitoring() {
        if (monitorThread != null) {
            return monitorThread;
        }
        monitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        try {
                            long current = System.currentTimeMillis();
                            MonitoredThread trigger = null;
                            for (MonitoredThread mt : monitoredThreads.values()) {
                                if ( (current - mt.startTime) > mt.alarmThreshold) {
                                    trigger = mt;
                                    break;
                                }
                            }
                            if (trigger != null) {
                                LOG.log(Level.WARNING, "Monitoring Alarm was triggered by {0} which ran for more than {1}ms  it is running since {2}ms ----------------------------------------",
                                        new Object[]{trigger.thread.getName(), trigger.alarmThreshold, current - trigger.startTime});
                                dumpThreads();
                                LOG.log(Level.WARNING, "end monitoring alarm thread dump -------------------------------------------------");
                            }
                        } catch (Exception ex) {
                            LOG.log(Level.SEVERE, null, ex);
                        }
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                        return;
                    }
                }
            }
        });
        monitorThread.start();
        return monitorThread;
    }
    
    

    private static void dumpThreads() {
        for (MonitoredThread mt : monitoredThreads.values()) {
            logThreadInfo(mt);
        }
    }

    private static void logThreadInfo(MonitoredThread mt) {
        StackTraceElement[] stack = mt.thread.getStackTrace();;
        long current = System.currentTimeMillis();
        long duration = current - mt.startTime;
        LOG.log(Level.WARNING, "thread {0} has run for {1}ms, alarm threshold is {2} reached threshold: {3} additional info: {4}",
                new Object[]{
                    mt.thread.getName(),
                    duration,
                    mt.alarmThreshold,
                    mt.alarmThreshold < duration,
                    mt.additionalInformation
                }
        );

        for (StackTraceElement el : stack) {
            LOG.log(Level.WARNING, "      {0}", el);
        }
    }

    public static void run(Runnable runnable, long alarmThreshold,String additionalInformation) {
        if(monitoredThreads.containsKey(Thread.currentThread())) {
            runnable.run();
            return;
        }
        MonitoredThread mt = new MonitoredThread();
        mt.thread = Thread.currentThread();
        mt.startTime = System.currentTimeMillis();
        mt.alarmThreshold = alarmThreshold;
        mt.additionalInformation=additionalInformation;
        try {
            monitoredThreads.put(mt.thread, mt);
            runnable.run();
        } finally {
            mt.endTime = System.currentTimeMillis();
            monitoredThreads.remove(mt.thread);
        }
    }
  
    private static class MonitoredThread {
        volatile Thread thread;
        volatile long startTime;
        volatile long alarmThreshold;
        volatile long endTime;
        volatile String additionalInformation;
    }

    private static final Logger LOG = getLogger(AutomaticMonitoring.class.getName());
}
