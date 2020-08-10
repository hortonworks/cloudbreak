package com.sequenceiq.periscope.service;

import java.util.Collection;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ClusterManagerVariant;
import com.sequenceiq.periscope.monitor.context.ClusterCreationEvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.ClusterCreationEvaluator;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;
import com.sequenceiq.periscope.service.configuration.CloudbreakClientConfiguration;
import com.sequenceiq.periscope.service.evaluator.ClusterCreationEvaluatorService;

@Service
public class StackCollectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCollectorService.class);

    private static final Lock LOCK = new ReentrantLock();

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private ExecutorServiceWithRegistry executorServiceWithRegistry;

    @Inject
    private CloudbreakClientConfiguration cloudbreakClientConfiguration;

    @Inject
    private RejectedThreadService rejectedThreadService;

    @Inject
    private ClusterCreationEvaluatorService clusterCreationEvaluatorService;

    public void collectStackDetails() {
        if (LOCK.tryLock()) {
            try {
                CloudbreakInternalCrnClient cloudbreakClient = cloudbreakClientConfiguration.cloudbreakInternalCrnClientClient();
                Collection<AutoscaleStackV4Response> allStacks = cloudbreakClient.withInternalCrn().autoscaleEndpoint().getAllForAutoscale().getResponses();
                for (AutoscaleStackV4Response stack : allStacks) {
                    try {
                        LOGGER.debug("Evaluate cluster management for stack: {} (ID:{})", stack.getName(), stack.getStackId());

                        ClusterManagerVariant variant = ClusterManagerVariant.valueOf(stack.getClusterManagerVariant());
                        Class<? extends ClusterCreationEvaluator> clusterCreationEvaluatorClass = clusterCreationEvaluatorService.get(variant);
                        ClusterCreationEvaluator clusterCreationEvaluator = applicationContext.getBean(clusterCreationEvaluatorClass);
                        clusterCreationEvaluator.setContext(new ClusterCreationEvaluatorContext(stack));
                        executorServiceWithRegistry.submitIfAbsent(clusterCreationEvaluator, stack.getStackId());
                        LOGGER.debug("Succesfully submitted, the stack id: {}.", stack.getStackId());
                        rejectedThreadService.remove(stack);
                    } catch (RejectedExecutionException ignore) {

                    }
                }
            } catch (Exception ex) {
                LOGGER.error("New clusters could not be synchronized from Cloudbreak.", ex);
            } finally {
                LOCK.unlock();
            }
        }
    }
}
