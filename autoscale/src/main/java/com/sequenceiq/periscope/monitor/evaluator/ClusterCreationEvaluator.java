package com.sequenceiq.periscope.monitor.evaluator;

import static com.sequenceiq.periscope.api.model.ClusterState.PENDING;
import static com.sequenceiq.periscope.api.model.ClusterState.SUSPENDED;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.api.model.AmbariAddressJson;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.periscope.domain.Ambari;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.domain.SecurityConfig;
import com.sequenceiq.periscope.model.AmbariStack;
import com.sequenceiq.periscope.model.ClusterCreationEvaluatorContext;
import com.sequenceiq.periscope.service.ClusterService;
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

    private ClusterCreationEvaluatorContext context;

    @Override
    public void run() {
        StackResponse stack = context.getStack();
        Optional<Cluster> clusterOptional = context.getClusterOptional();
        try {
            createOrUpdateCluster(stack, clusterOptional);
        } catch (AmbariHealtCheckFailed ahf) {
            LOGGER.warn(String.format("Ambari health check failed for Cloudbreak stack: %s(ID:%s)", stack.getId(), stack.getName()), ahf);
        } catch (TlsConfigurationException ex) {
            LOGGER.warn(String.format("Could not prepare TLS configuration for Cloudbreak stack: %s(ID:%s)", stack.getId(), stack.getName()), ex);
        } catch (Exception ex) {
            LOGGER.warn(String.format("Could not create cluster for Cloudbreak stack: %s(ID:%s)", stack.getId(), stack.getName()), ex);
        }
    }

    public void setContext(ClusterCreationEvaluatorContext context) {
        this.context = context;
    }

    private void createOrUpdateCluster(StackResponse stack, Optional<Cluster> clusterOptional) {
        AmbariStack resolvedAmbari = createAmbariStack(stack);
        if (clusterOptional.isPresent()) {
            Cluster cluster = clusterOptional.get();
            if (PENDING.equals(cluster.getState()) || SUSPENDED.equals(cluster.getState())) {
                LOGGER.info("Creating cluster for Ambari host: {}", resolvedAmbari.getAmbari().getHost());
                ambariHealthCheck(cluster.getUser(), resolvedAmbari);
                clusterService.update(cluster.getId(), resolvedAmbari);
            }
        } else {
            LOGGER.info("Creating cluster for Ambari host: {}", resolvedAmbari.getAmbari().getHost());
            PeriscopeUser user = new PeriscopeUser(stack.getOwner(), null, stack.getAccount());
            ambariHealthCheck(user, resolvedAmbari);
            clusterService.create(user, resolvedAmbari, null);
        }
    }

    private AmbariStack createAmbariStack(StackResponse stack) {
        ClusterResponse cluster = stack.getCluster();
        String host = cluster.getAmbariServerIp();
        AmbariAddressJson ambariAddressJson = new AmbariAddressJson();
        ambariAddressJson.setAmbariAddress(host);
        String gatewayPort = String.valueOf(stack.getGatewayPort());
        SecurityConfig securityConfig = tlsSecurityService.prepareSecurityConfig(stack.getId());
        return new AmbariStack(new Ambari(host, gatewayPort, cluster.getUserName(), cluster.getPassword()), stack.getId(), securityConfig);
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

    private class AmbariHealtCheckFailed extends RuntimeException {
        AmbariHealtCheckFailed(String message) {
            super(message);
        }

        AmbariHealtCheckFailed(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
