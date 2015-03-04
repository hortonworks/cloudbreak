package com.sequenceiq.periscope.service.security;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.periscope.domain.Ambari;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.model.AmbariStack;
import com.sequenceiq.periscope.service.CloudbreakService;

@Service
public class ClusterSecurityService {

    @Autowired
    private CloudbreakService cloudbreakService;

    public boolean hasAccess(PeriscopeUser user, Ambari ambari) {
        CloudbreakClient client = cloudbreakService.getClient();
        try {
            return client.hasAccess(user.getId(), user.getAccount(), ambari.getHost());
        } catch (Exception e) {
            // if the cluster is unknown for cloudbreak
            // it should allow it to monitor
            return true;
        }
    }

    public AmbariStack tryResolve(Ambari ambari) {
        CloudbreakClient client = cloudbreakService.getClient();
        try {
            String host = ambari.getHost();
            String user = ambari.getUser();
            String pass = ambari.getPass();
            long id = client.resolveToStackId(host);
            if (user == null && pass == null) {
                Map<String, String> stack = (Map<String, String>) client.getStack("" + id);
                return new AmbariStack(new Ambari(host, ambari.getPort(), stack.get("userName"), stack.get("password")), id);
            } else {
                return new AmbariStack(ambari, id);
            }
        } catch (Exception e) {
            return new AmbariStack(ambari);
        }
    }

}
