package org.example.server.config;

public class ThreadConfigContext {
    private static final ThreadLocal<ThreadConfig> threadLocals = new ThreadLocal<>();
    public static void set(ThreadConfig threadConfig) {
        threadLocals.set(threadConfig);
    }
    public static ThreadConfig get() {
        return threadLocals.get();
    }
    public static void clear() {
        threadLocals.remove();
    }
}
