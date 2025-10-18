package org.news.api.config;

import org.springframework.stereotype.Component;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class SchedulerLock {
    public static final ReentrantLock LOCK = new ReentrantLock();
}
