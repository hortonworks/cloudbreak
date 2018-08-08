package com.sequenceiq.periscope.service;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.periscope.monitor.context.ClusterCreationEvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.ClusterCreationEvaluator;
import com.sequenceiq.periscope.service.configuration.CloudbreakClientConfiguration;

@Service
public class StackCollectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCollectorService.class);

    private static final Lock LOCK = new ReentrantLock();

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private ExecutorService executorService;

    @Inject
    private CloudbreakClientConfiguration cloudbreakClientConfiguration;

    @Inject
    private RejectedThreadService rejectedThreadService;

    public void collectStackDetails() {
        if (LOCK.tryLock()) {
            try {
                CloudbreakClient cloudbreakClient = cloudbreakClientConfiguration.cloudbreakClient();
                Set<AutoscaleStackResponse> allStacks = cloudbreakClient.stackV1Endpoint().getAllForAutoscale();
                for (AutoscaleStackResponse stack : allStacks) {
                    Status clusterStatus = stack.getClusterStatus();
                    if (AVAILABLE.equals(clusterStatus)) {
                        if (stack.getAmbariServerIp() != null) {
                            try {
                                LOGGER.info("Evaluate cluster management for stack: {} (ID:{})", stack.getName(), stack.getStackId());
                                ClusterCreationEvaluator clusterCreationEvaluator = applicationContext.getBean(ClusterCreationEvaluator.class);
                                clusterCreationEvaluator.setContext(new ClusterCreationEvaluatorContext(stack));
                                executorService.submit(clusterCreationEvaluator);
                                LOGGER.info("Succesfully submitted, the stack id: {}. Executor: {}", stack.getStackId(), executorService);
                                rejectedThreadService.remove(stack);
                            } catch (RejectedExecutionException ignore) {

                            }
                        } else {
                            LOGGER.info("Could not find Ambari for stack: {} (ID:{})", stack.getName(), stack.getStackId());
                        }
                    } else {
                        LOGGER.info("Do not create or update cluster while the Cloudbreak cluster {} (ID:{}) is in '{}' state instead of 'AVAILABLE'!",
                                stack.getName(), stack.getStackId(), stack.getClusterStatus());
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
