package com.sequenceiq.periscope.monitor.evaluator;

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
import com.sequenceiq.periscope.aspects.AmbariRequestLogging;
import com.sequenceiq.periscope.domain.Ambari;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.SecurityConfig;
import com.sequenceiq.periscope.model.AmbariStack;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.service.AmbariClientProvider;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.security.TlsConfigurationException;
import com.sequenceiq.periscope.service.security.TlsSecurityService;

@Component("ClusterCreationEvaluator")
@Scope("prototype")
public class ClusterCreationEvaluator extends EvaluatorExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationEvaluator.class);

    private static final String EVALUATOR_NAME = ClusterCreationEvaluator.class.getName();

    @Inject
    private ClusterService clusterService;

    @Inject
    private AmbariClientProvider ambariClientProvider;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private HistoryService historyService;

    @Inject
    private HttpNotificationSender notificationSender;

    @Inject
    private AmbariRequestLogging ambariRequestLogging;

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
            AmbariStack resolvedAmbari = createAmbariStack(stack);
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

    private void createCluster(AutoscaleStackV4Response stack, AmbariStack resolvedAmbari) {
        MDCBuilder.buildMdcContext(stack.getStackId(), stack.getName(), "CLUSTER");
        LOGGER.debug("Creating cluster for Ambari host: {}", resolvedAmbari.getAmbari().getHost());
        Cluster cluster = clusterService.create(resolvedAmbari, null,
                new ClusterPertain(stack.getTenant(), stack.getWorkspaceId(), stack.getUserId()));
        History history = historyService.createEntry(ScalingStatus.ENABLED, "Autoscaling has been enabled for the cluster.", 0, cluster);
        notificationSender.send(cluster, history);
    }

    private void updateCluster(AutoscaleStackV4Response stack, Cluster cluster, AmbariStack resolvedAmbari) {
        if (PENDING.equals(cluster.getState()) || SUSPENDED.equals(cluster.getState())) {
            MDCBuilder.buildMdcContext(cluster);
            LOGGER.debug("Update cluster and set it's state to 'RUNNING' for Ambari host: {}", resolvedAmbari.getAmbari().getHost());
            cluster = clusterService.update(cluster.getId(), resolvedAmbari, RUNNING, cluster.isAutoscalingEnabled());
            History history = historyService.createEntry(ScalingStatus.ENABLED, "Autoscaling has been enabled for the cluster.", 0, cluster);
            notificationSender.send(cluster, history);
        }
    }

    private AmbariStack createAmbariStack(AutoscaleStackV4Response stack) {
        String host = stack.getAmbariServerIp();
        String gatewayPort = String.valueOf(stack.getGatewayPort());
        SecurityConfig securityConfig = tlsSecurityService.prepareSecurityConfig(stack.getStackId());
        return new AmbariStack(new Ambari(host, gatewayPort, stack.getUserNamePath(), stack.getPasswordPath()), stack.getStackId(), securityConfig);
    }

    private void ambariHealthCheck(AmbariStack ambariStack) {
        String host = ambariStack.getAmbari().getHost();
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

    private static class AmbariHealthCheckException extends RuntimeException {
        AmbariHealthCheckException(String message) {
            super(message);
        }

        AmbariHealthCheckException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
