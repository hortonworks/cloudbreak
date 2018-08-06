package com.sequenceiq.periscope.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class LoggerUtils {
    public void logThreadPoolExecutorParameters(Logger logger, String monitorClassName, ExecutorService executorService) {
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;
        logger.info("threadpool tasks: monitor class is: {}, active threads: {}, poolsize: {}, queueSize: {}, completed tasks {}",
                monitorClassName,
                threadPoolExecutor.getActiveCount(),
                threadPoolExecutor.getPoolSize(),
                threadPoolExecutor.getQueue().size(),
                threadPoolExecutor.getCompletedTaskCount()
        );
    }

}
