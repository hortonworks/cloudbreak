package com.sequenceiq.periscope.service.security;

import java.util.HashSet;

import javax.inject.Inject;

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

    @Inject
    private CloudbreakClient cloudbreakClient;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public boolean hasAccess(PeriscopeUser user, Ambari ambari, Long stackId) {
        try {
            return hasAccess(user.getId(), user.getAccount(), ambari.getHost(), stackId);
        } catch (RuntimeException ignored) {
            // if the cluster is unknown for cloudbreak
            // it should allow it to monitor
            return true;
        }
    }

    private boolean hasAccess(String userId, String account, String ambariAddress, Long stackId) {
        StackResponse stack;
        if (stackId != null) {
            stack = cloudbreakClient.stackV1Endpoint().get(stackId, new HashSet<>());
        } else {
            AmbariAddressJson ambariAddressJson = new AmbariAddressJson();
            ambariAddressJson.setAmbariAddress(ambariAddress);
            stack = cloudbreakClient.stackV1Endpoint().getStackForAmbari(ambariAddressJson);
        }
        return stack.getOwner().equals(userId) || (stack.isPublicInAccount() && stack.getAccount().equals(account));
    }

    public AmbariStack tryResolve(Ambari ambari) {
        try {
            String host = ambari.getHost();
            String user = ambari.getUser();
            String pass = ambari.getPass();
            AmbariAddressJson ambariAddressJson = new AmbariAddressJson();
            ambariAddressJson.setAmbariAddress(host);
            StackResponse stack = cloudbreakClient.stackV1Endpoint().getStackForAmbari(ambariAddressJson);
            Long id = stack.getId();
            SecurityConfig securityConfig = tlsSecurityService.prepareSecurityConfig(id);
            if (user == null || pass == null) {
                AutoscaleClusterResponse clusterResponse = cloudbreakClient.clusterEndpoint().getForAutoscale(id);
                return new AmbariStack(new Ambari(host, ambari.getPort(), clusterResponse.getUserName(), clusterResponse.getPassword()), id, securityConfig);
            } else {
                return new AmbariStack(ambari, id, securityConfig);
            }
        } catch (RuntimeException ignored) {
            return new AmbariStack(ambari);
        }
    }

}
