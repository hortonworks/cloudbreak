package com.sequenceiq.periscope.monitor.evaluator;

import static com.sequenceiq.periscope.api.model.ClusterState.PENDING;
import static com.sequenceiq.periscope.api.model.ClusterState.RUNNING;
import static com.sequenceiq.periscope.api.model.ClusterState.SUSPENDED;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.aspects.AmbariRequestLogging;
import com.sequenceiq.periscope.domain.Ambari;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.domain.SecurityConfig;
import com.sequenceiq.periscope.log.MDCBuilder;
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
        AutoscaleStackResponse stack = (AutoscaleStackResponse) context.getData();
        try {
            Cluster cluster = clusterService.findOneByStackId(stack.getStackId());
            AmbariStack resolvedAmbari = createAmbariStack(stack);
            if (cluster != null) {
                ambariHealthCheck(cluster.getUser(), resolvedAmbari);
                updateCluster(stack, cluster, resolvedAmbari);
            } else {
                clusterService.validateClusterUniqueness(resolvedAmbari);
                createCluster(stack, resolvedAmbari);
            }
        } catch (AmbariHealtCheckException ahf) {
            LOGGER.warn("Ambari health check failed for Cloudbreak stack: {} (ID:{}). Original message: {}", stack.getStackId(), stack.getName(),
                    ahf.getMessage());
        } catch (TlsConfigurationException ex) {
            LOGGER.warn("Could not prepare TLS configuration for Cloudbreak stack: {} (ID:{}). Original message: {}", stack.getStackId(), stack.getName(),
                    ex.getMessage());
        } catch (Exception ex) {
            LOGGER.error(String.format("Could not create cluster for Cloudbreak stack: %s (ID:%s)", stack.getStackId(), stack.getName()), ex);
        } finally {
            LOGGER.info("Finished clusterCreationEvaluator in {} ms", System.currentTimeMillis() - start);
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

    private void createCluster(AutoscaleStackResponse stack, AmbariStack resolvedAmbari) {
        PeriscopeUser user = new PeriscopeUser(stack.getOwner(), null, stack.getAccount());
        MDCBuilder.buildMdcContext(user, stack.getStackId(), null);
        LOGGER.info("Creating cluster for Ambari host: {}", resolvedAmbari.getAmbari().getHost());
        Cluster cluster = clusterService.create(user, resolvedAmbari, null);
        History history = historyService.createEntry(ScalingStatus.ENABLED, "Autoscaling has been enabled for the cluster.", 0, cluster);
        notificationSender.send(history);
    }

    private void updateCluster(AutoscaleStackResponse stack, Cluster cluster, AmbariStack resolvedAmbari) {
        if (PENDING.equals(cluster.getState()) || SUSPENDED.equals(cluster.getState())) {
            MDCBuilder.buildMdcContext(cluster);
            LOGGER.info("Update cluster and set it's state to 'RUNNING' for Ambari host: {}", resolvedAmbari.getAmbari().getHost());
            cluster = clusterService.update(cluster.getId(), resolvedAmbari, RUNNING, cluster.isAutoscalingEnabled());
            History history = historyService.createEntry(ScalingStatus.ENABLED, "Autoscaling has been enabled for the cluster.", 0, cluster);
            notificationSender.send(history);
        }
    }

    private AmbariStack createAmbariStack(AutoscaleStackResponse stack) {
        String host = stack.getAmbariServerIp();
        String gatewayPort = String.valueOf(stack.getGatewayPort());
        SecurityConfig securityConfig = tlsSecurityService.prepareSecurityConfig(stack.getStackId());
        return new AmbariStack(new Ambari(host, gatewayPort, stack.getUserNamePath(), stack.getPasswordPath()), stack.getStackId(), securityConfig);
    }

    private void ambariHealthCheck(PeriscopeUser user, AmbariStack ambariStack) {
        String host = ambariStack.getAmbari().getHost();
        try {
            AmbariClient client = ambariClientProvider.createAmbariClient(new Cluster(user, ambariStack));
            String healthCheckResult = ambariRequestLogging.logging(client::healthCheck, "healthCheck");
            if (!"RUNNING".equals(healthCheckResult)) {
                throw new AmbariHealtCheckException(String.format("Ambari on host '%s' is not in 'RUNNING' state.", host));
            }
        } catch (Exception ex) {
            throw new AmbariHealtCheckException(String.format("Health check failed on host '%s':", host), ex);
        }
    }

    private static class AmbariHealtCheckException extends RuntimeException {
        AmbariHealtCheckException(String message) {
            super(message);
        }

        AmbariHealtCheckException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
