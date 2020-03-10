package com.sequenceiq.periscope.monitor.evaluator.ambari;

import static com.sequenceiq.periscope.api.model.ClusterState.PENDING;
import static com.sequenceiq.periscope.api.model.ClusterState.RUNNING;
import static com.sequenceiq.periscope.api.model.ClusterState.SUSPENDED;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.aspects.RequestLogging;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterManager;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ClusterManagerVariant;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.SecurityConfig;
import com.sequenceiq.periscope.model.MonitoredStack;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.ClusterCreationEvaluator;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.service.AmbariClientProvider;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.security.SecurityConfigService;
import com.sequenceiq.periscope.service.security.TlsConfigurationException;

@Component("AmbariClusterCreationEvaluator")
@Scope("prototype")
public class AmbariClusterCreationEvaluator extends ClusterCreationEvaluator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterCreationEvaluator.class);

    private static final String EVALUATOR_NAME = AmbariClusterCreationEvaluator.class.getName();

    @Inject
    private ClusterService clusterService;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private HistoryService historyService;

    @Inject
    private HttpNotificationSender notificationSender;

    @Inject
    private RequestLogging ambariRequestLogging;

    private EvaluatorContext context;

    @Override
    public String getName() {
        return EVALUATOR_NAME;
    }

    @Override
    public void execute() {
        long start = System.currentTimeMillis();
        AutoscaleStackV4Response stack = (AutoscaleStackV4Response) context.getData();
        try {
            Cluster cluster = clusterService.findOneByStackId(stack.getStackId());
            Long clusterId = null;
            if (cluster != null) {
                clusterId = cluster.getId();
            }
            MonitoredStack resolvedAmbari = createAmbariStack(stack, clusterId);
            if (cluster != null) {
                ambariHealthCheck(resolvedAmbari);
                updateCluster(stack, cluster, resolvedAmbari);
            } else {
                clusterService.validateClusterUniqueness(resolvedAmbari);
                createCluster(stack, resolvedAmbari);
            }
        } catch (AmbariHealthCheckException ahf) {
            LOGGER.info("Ambari health check failed for Cloudbreak stack: {} (ID:{}). Original message: {}", stack.getStackId(), stack.getName(),
                    ahf.getMessage());
        } catch (TlsConfigurationException ex) {
            LOGGER.error("Could not prepare TLS configuration for Cloudbreak stack: {} (ID:{}). Original message: {}", stack.getStackId(), stack.getName(),
                    ex.getMessage());
        } catch (Exception ex) {
            LOGGER.warn(String.format("Could not create cluster for Cloudbreak stack: %s (ID:%s)", stack.getStackId(), stack.getName()), ex);
        } finally {
            LOGGER.debug("Finished clusterCreationEvaluator in {} ms", System.currentTimeMillis() - start);
        }
    }

    @Override
    public void setContext(EvaluatorContext context) {
        this.context = context;
    }

    @Override
    @Nonnull
    public EvaluatorContext getContext() {
        return context;
    }

    private void createCluster(AutoscaleStackV4Response stack, MonitoredStack resolvedAmbari) {
        LOGGER.debug("Creating cluster for Ambari host: {}", resolvedAmbari.getClusterManager().getHost());
        Cluster cluster = clusterService.create(resolvedAmbari, null,
                new ClusterPertain(stack.getTenant(), stack.getWorkspaceId(), stack.getUserId()));
        MDCBuilder.buildMdcContext(cluster);
        History history = historyService.createEntry(ScalingStatus.ENABLED, "Autoscaling has been enabled for the cluster.", 0, cluster);
        notificationSender.send(cluster, history);
    }

    private void updateCluster(AutoscaleStackV4Response stack, Cluster cluster, MonitoredStack resolvedAmbari) {
        if (PENDING.equals(cluster.getState()) || SUSPENDED.equals(cluster.getState())) {
            MDCBuilder.buildMdcContext(cluster);
            LOGGER.debug("Update cluster and set it's state to 'RUNNING' for Ambari host: {}", resolvedAmbari.getClusterManager().getHost());
            cluster = clusterService.update(cluster.getId(), resolvedAmbari, RUNNING, cluster.isAutoscalingEnabled());
            History history = historyService.createEntry(ScalingStatus.ENABLED, "Autoscaling has been enabled for the cluster.", 0, cluster);
            notificationSender.send(cluster, history);
        }
    }

    private MonitoredStack createAmbariStack(AutoscaleStackV4Response stack, Long clusterId) {
        String host = stack.getAmbariServerIp();
        String gatewayPort = String.valueOf(stack.getGatewayPort());
        SecurityConfig securityConfig = null;
        if (clusterId != null) {
            securityConfig = securityConfigService.getSecurityConfig(clusterId);
        }
        ClusterManager clusterManager = new ClusterManager(host, gatewayPort, stack.getUserNamePath(), stack.getPasswordPath(), ClusterManagerVariant.AMBARI);
        return new MonitoredStack(clusterManager, stack.getName(), stack.getStackCrn(), stack.getStackType(),
                stack.getStackId(), securityConfig, stack.getTunnel());
    }

    private void ambariHealthCheck(MonitoredStack ambariStack) {
        String host = ambariStack.getClusterManager().getHost();
        try {
            AmbariClient client = ambariClientProvider.createAmbariClient(new Cluster(ambariStack));
            String healthCheckResult = ambariRequestLogging.logging(() -> {
                try {
                    return client.healthCheck();
                } catch (IOException | URISyntaxException e) {
                    throw new AmbariHealthCheckException("Failed to connect Ambari host", e);
                }
            }, "healthCheck");
            if (!"RUNNING".equals(healthCheckResult)) {
                throw new AmbariHealthCheckException(String.format("Ambari on host '%s' is not in 'RUNNING' state.", host));
            }
        } catch (Exception ex) {
            throw new AmbariHealthCheckException(String.format("Health check failed on host '%s':", host), ex);
        }
    }

    @Override
    public ClusterManagerVariant getSupportedClusterManagerVariant() {
        return ClusterManagerVariant.AMBARI;
    }

    private static class AmbariHealthCheckException extends RuntimeException {
        AmbariHealthCheckException(String message) {
            super(message);
        }

        AmbariHealthCheckException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
