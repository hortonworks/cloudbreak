package com.sequenceiq.periscope.monitor;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.model.ClusterCreationEvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.ClusterCreationEvaluator;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorContext;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.configuration.CloudbreakClientConfiguration;

@Component
public class ClusterMonitor implements Monitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterMonitor.class);

    private ApplicationContext applicationContext;

    private ExecutorService executorService;

    @Override
    public String getIdentifier() {
        return "cluster-monitor";
    }

    @Override
    public String getTriggerExpression() {
        return MonitorUpdateRate.EVERY_MIN_RATE_CRON;
    }

    @Override
    public Class<?> getEvaluatorType() {
        return ClusterCreationEvaluator.class;
    }

    @Override
    public Map<String, Object> getContext(Cluster cluster) {
        return Collections.singletonMap(EvaluatorContext.CLUSTER_ID.name(), cluster.getId());
    }

    @Override
    public void execute(JobExecutionContext context) {
        evalContext(context);
        try {
            CloudbreakClient cloudbreakClient = applicationContext.getBean(CloudbreakClientConfiguration.class).cloudbreakClient();
            ClusterService clusterService = applicationContext.getBean(ClusterService.class);
            List<Cluster> clusters = clusterService.findAll();
            Set<AutoscaleStackResponse> allStacks = cloudbreakClient.stackV1Endpoint().getAllForAutoscale();

            for (AutoscaleStackResponse stack : allStacks) {
                Status clusterStatus = stack.getClusterStatus();
                if (clusterStatus != null && AVAILABLE.equals(clusterStatus)) {
                    String ambariIp = stack.getAmbariServerIp();
                    Optional<Cluster> clusterOptional = clusters.stream()
                            .filter(c -> c.getStackId() != null && c.getStackId().equals(stack.getStackId())).findFirst();
                    if (ambariIp != null) {
                        ClusterCreationEvaluator clusterCreationEvaluator = applicationContext.getBean(ClusterCreationEvaluator.class);
                        clusterCreationEvaluator.setContext(new ClusterCreationEvaluatorContext(stack, clusterOptional));
                        executorService.submit(clusterCreationEvaluator);
                    } else {
                        LOGGER.info("Could not find Ambari for stack: {}(ID:{})", stack.getName(), stack.getStackId());
                    }
                } else {
                    LOGGER.info("Do not create or update cluster while the Cloudbreak cluster {}(ID:{}) is in '{}' state instead of 'AVAILABLE'!",
                            stack.getName(), stack.getStackId(), stack.getClusterStatus());
                }
            }
        } catch (Exception ex) {
            LOGGER.error("New clusters could not be synchronized from Cloudbreak.", ex);
        }

    }

    private void evalContext(JobExecutionContext context) {
        JobDataMap monitorContext = context.getJobDetail().getJobDataMap();
        applicationContext = (ApplicationContext) monitorContext.get(MonitorContext.APPLICATION_CONTEXT.name());
        executorService = applicationContext.getBean(ExecutorService.class);
    }
}
