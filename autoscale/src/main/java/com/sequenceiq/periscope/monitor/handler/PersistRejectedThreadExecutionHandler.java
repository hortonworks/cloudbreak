package com.sequenceiq.periscope.monitor.handler;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;
import com.sequenceiq.periscope.service.RejectedThreadService;

@Component
public class PersistRejectedThreadExecutionHandler extends AbortPolicy {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistRejectedThreadExecutionHandler.class);

    private final Field callableInFutureTask;

    private final Class<? extends Callable> adapterClass;

    private final Field runnableInAdapter;

    @Inject
    private RejectedThreadService rejectedThreadService;

    public PersistRejectedThreadExecutionHandler() {
        try {
            callableInFutureTask = FutureTask.class.getDeclaredField("callable");
            callableInFutureTask.setAccessible(true);
            adapterClass = Executors.callable(() -> {
            }).getClass();
            runnableInAdapter = adapterClass.getDeclaredField("task");
            runnableInAdapter.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Object findRealTask(Runnable task) {
        if (task instanceof FutureTask) {
            try {
                Object callable = callableInFutureTask.get(task);
                if (adapterClass.isInstance(callable)) {
                    return runnableInAdapter.get(callable);
                } else {
                    return callable;
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        return null;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        Object realTask = findRealTask(r);
        if (realTask instanceof EvaluatorExecutor) {
            LOGGER.info("Total count of rejected tasks: {}", rejectedThreadService.getAllRejectedCluster().size());
            LOGGER.info("Thread is rejected: {} from {}", realTask, executor);
            rejectedThreadService.create((EvaluatorExecutor) realTask);
            super.rejectedExecution(r, executor);
        }
    }

    private Object getObject(EvaluatorExecutor evaluatorExecutor) {
        Object data = evaluatorExecutor.getContext().getData();
        if (data instanceof AutoscaleStackResponse) {
            return data;
        } else {
            Cluster cluster = new Cluster();
            cluster.setId((long) data);
            return cluster;
        }
    }
}
