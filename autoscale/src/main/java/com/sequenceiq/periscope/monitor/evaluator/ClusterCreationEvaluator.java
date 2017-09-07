package com.sequenceiq.periscope.monitor.evaluator;

import static com.sequenceiq.periscope.api.model.ClusterState.PENDING;
import static com.sequenceiq.periscope.api.model.ClusterState.RUNNING;
import static com.sequenceiq.periscope.api.model.ClusterState.SUSPENDED;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.model.AmbariAddressJson;
import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.domain.Ambari;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.domain.SecurityConfig;
import com.sequenceiq.periscope.log.MDCBuilder;
import com.sequenceiq.periscope.model.AmbariStack;
import com.sequenceiq.periscope.model.ClusterCreationEvaluatorContext;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.security.TlsConfigurationException;
import com.sequenceiq.periscope.service.security.TlsSecurityService;
import com.sequenceiq.periscope.utils.AmbariClientProvider;

@Component("ClusterCreationEvaluator")
@Scope("prototype")
public class ClusterCreationEvaluator implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationEvaluator.class);

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

    private ClusterCreationEvaluatorContext context;

    @Override
    public void run() {
        AutoscaleStackResponse stack = context.getStack();
        Optional<Cluster> clusterOptional = context.getClusterOptional();
        try {
            createOrUpdateCluster(stack, clusterOptional);
        } catch (AmbariHealtCheckFailed ahf) {
            LOGGER.warn(String.format("Ambari health check failed for Cloudbreak stack: %s(ID:%s)", stack.getStackId(), stack.getName()), ahf);
        } catch (TlsConfigurationException ex) {
            LOGGER.warn(String.format("Could not prepare TLS configuration for Cloudbreak stack: %s(ID:%s)", stack.getStackId(), stack.getName()), ex);
        } catch (Exception ex) {
            LOGGER.warn(String.format("Could not create cluster for Cloudbreak stack: %s(ID:%s)", stack.getStackId(), stack.getName()), ex);
        }
    }

    public void setContext(ClusterCreationEvaluatorContext context) {
        this.context = context;
    }

    private void createOrUpdateCluster(AutoscaleStackResponse stack, Optional<Cluster> clusterOptional) {
        AmbariStack resolvedAmbari = createAmbariStack(stack);
        Cluster cluster;
        boolean sendNotification = false;
        if (clusterOptional.isPresent()) {
            cluster = clusterOptional.get();
            MDCBuilder.buildMdcContext(cluster);
            if (PENDING.equals(cluster.getState()) || SUSPENDED.equals(cluster.getState())) {
                ambariHealthCheck(cluster.getUser(), resolvedAmbari);
                LOGGER.info("Update cluster and set it's state to 'RUNNING' for Ambari host: {}", resolvedAmbari.getAmbari().getHost());
                cluster = clusterService.update(cluster.getId(), resolvedAmbari, false, RUNNING, cluster.isAutoscalingEnabled());
                sendNotification = true;
            }
        } else {
            PeriscopeUser user = new PeriscopeUser(stack.getOwner(), null, stack.getAccount());
            MDCBuilder.buildMdcContext(user, stack.getStackId(), null);
            LOGGER.info("Creating cluster for Ambari host: {}", resolvedAmbari.getAmbari().getHost());
            ambariHealthCheck(user, resolvedAmbari);
            cluster = clusterService.create(user, resolvedAmbari, null);
            sendNotification = true;
        }
        if (sendNotification) {
            History history = historyService.createEntry(ScalingStatus.ENABLED, "Autoscaling has been enabled for the cluster.", 0, cluster);
            notificationSender.send(history);
        }
    }

    private AmbariStack createAmbariStack(AutoscaleStackResponse stack) {
        String host = stack.getAmbariServerIp();
        AmbariAddressJson ambariAddressJson = new AmbariAddressJson();
        ambariAddressJson.setAmbariAddress(host);
        String gatewayPort = String.valueOf(stack.getGatewayPort());
        SecurityConfig securityConfig = tlsSecurityService.prepareSecurityConfig(stack.getStackId());
        return new AmbariStack(new Ambari(host, gatewayPort, stack.getUserName(), stack.getPassword()), stack.getStackId(), securityConfig);
    }

    private void ambariHealthCheck(PeriscopeUser user, AmbariStack ambariStack) {
        String host = ambariStack.getAmbari().getHost();
        try {
            AmbariClient client = ambariClientProvider.createAmbariClient(new Cluster(user, ambariStack));
            String healthCheckResult = client.healthCheck();
            if (!"RUNNING".equals(healthCheckResult)) {
                throw new AmbariHealtCheckFailed(String.format("Ambari on host '%s' is not in 'RUNNING' state.", host));
            }
        } catch (Exception ex) {
            throw new AmbariHealtCheckFailed(String.format("Health check failed on host '%s':", host), ex);
        }
    }

    private static class AmbariHealtCheckFailed extends RuntimeException {
        AmbariHealtCheckFailed(String message) {
            super(message);
        }

        AmbariHealtCheckFailed(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
