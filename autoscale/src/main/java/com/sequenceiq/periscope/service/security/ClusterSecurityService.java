package com.sequenceiq.periscope.service.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.AmbariAddressJson;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.periscope.domain.Ambari;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.domain.SecurityConfig;
import com.sequenceiq.periscope.model.AmbariStack;
import com.sequenceiq.periscope.model.CloudbreakClient;

@Service
public class ClusterSecurityService {

    @Autowired
    private CloudbreakClient cloudbreakClient;

    @Autowired
    private TlsSecurityService tlsSecurityService;

    public boolean hasAccess(PeriscopeUser user, Ambari ambari) {
        try {
            return hasAccess(user.getId(), user.getAccount(), ambari.getHost());
        } catch (Exception e) {
            // if the cluster is unknown for cloudbreak
            // it should allow it to monitor
            return true;
        }
    }

    private Boolean hasAccess(String userId, String account, String ambariAddress) throws Exception {
        AmbariAddressJson ambariAddressJson = new AmbariAddressJson();
        ambariAddressJson.setAmbariAddress(ambariAddress);
        StackResponse stack = cloudbreakClient.stackEndpoint().getStackForAmbari(ambariAddressJson);
        if (stack.getOwner().equals(userId)) {
            return true;
        } else if (stack.isPublicInAccount() && stack.getAccount() == account) {
            return true;
        }
        return false;
    }

    public AmbariStack tryResolve(Ambari ambari) {
        try {
            String host = ambari.getHost();
            String user = ambari.getUser();
            String pass = ambari.getPass();
            AmbariAddressJson ambariAddressJson = new AmbariAddressJson();
            ambariAddressJson.setAmbariAddress(host);
            StackResponse stack = cloudbreakClient.stackEndpoint().getStackForAmbari(ambariAddressJson);
            long id = stack.getId();
            SecurityConfig securityConfig = tlsSecurityService.prepareSecurityConfig(id);
            if (user == null && pass == null) {
                ClusterResponse clusterResponse = cloudbreakClient.clusterEndpoint().get(id);
                return new AmbariStack(new Ambari(host, ambari.getPort(), clusterResponse.getUserName(), clusterResponse.getPassword()), id, securityConfig);
            } else {
                return new AmbariStack(ambari, id, securityConfig);
            }
        } catch (Exception e) {
            return new AmbariStack(ambari);
        }
    }

}
