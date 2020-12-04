package com.sequenceiq.cloudbreak.cloud.azure.image.pageblobv12.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolManager {

    private final List<ThreadPoolExecutor> threadPoolExecutorList = new ArrayList<>();

    public ThreadPoolManager(int threadPoolCount, int threadPoolSize) {
        for (int i = 0; i < threadPoolCount; i++) {
            threadPoolExecutorList.add((ThreadPoolExecutor) Executors.newFixedThreadPool(threadPoolSize));
        }
    }

    public ThreadPoolExecutor get(int jobId) {
        System.out.println(jobId % threadPoolExecutorList.size());
        return threadPoolExecutorList.get(jobId % threadPoolExecutorList.size());
    }
}
