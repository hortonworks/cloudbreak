package com.sequenceiq.periscope.service.security;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.AmbariAddressJson;
import com.sequenceiq.cloudbreak.api.model.AutoscaleClusterResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.periscope.domain.Ambari;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.domain.SecurityConfig;
import com.sequenceiq.periscope.model.AmbariStack;

@Service
public class ClusterSecurityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterSecurityService.class);

    @Inject
    private CloudbreakClient cloudbreakClient;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public boolean hasAccess(PeriscopeUser user, Ambari ambari, Long stackId) {
        try {
            return hasAccess(user.getId(), user.getAccount(), ambari.getHost(), stackId);
        } catch (RuntimeException e) {
            // if the cluster is unknown for cloudbreak
            // it should allow it to monitor
            LOGGER.debug("hasAccess request failed, falling back to 'true'", e);
            return true;
        }
    }

    private boolean hasAccess(String userId, String account, String ambariAddress, Long stackId) {
        StackResponse stack;
        if (stackId != null) {
            stack = cloudbreakClient.autoscaleEndpoint().get(stackId);
        } else {
            AmbariAddressJson ambariAddressJson = new AmbariAddressJson();
            ambariAddressJson.setAmbariAddress(ambariAddress);
            stack = cloudbreakClient.autoscaleEndpoint().getStackForAmbari(ambariAddressJson);
        }
        return stack.getOwner().equals(userId) || stack.getAccount().equals(account);
    }

    public AmbariStack tryResolve(Ambari ambari) {
        try {
            String host = ambari.getHost();
            String user = ambari.getUser();
            String pass = ambari.getPass();
            AmbariAddressJson ambariAddressJson = new AmbariAddressJson();
            ambariAddressJson.setAmbariAddress(host);
            StackResponse stack = cloudbreakClient.autoscaleEndpoint().getStackForAmbari(ambariAddressJson);
            Long id = stack.getId();
            SecurityConfig securityConfig = tlsSecurityService.prepareSecurityConfig(id);
            if (user == null || pass == null) {
                AutoscaleClusterResponse clusterResponse = cloudbreakClient.autoscaleEndpoint().getForAutoscale(id);
                return new AmbariStack(new Ambari(host, ambari.getPort(), clusterResponse.getUserName(), clusterResponse.getPassword()), id, securityConfig);
            } else {
                return new AmbariStack(ambari, id, securityConfig);
            }
        } catch (RuntimeException ignored) {
            return new AmbariStack(ambari);
        }
    }

}
