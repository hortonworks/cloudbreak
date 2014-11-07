package com.sequenceiq.periscope.service.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.periscope.domain.Ambari;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.service.CloudbreakService;

import groovyx.net.http.HttpResponseException;

@Service
public class ClusterSecurityService {

    @Autowired
    private CloudbreakService cloudbreakService;

    public boolean hasAccess(PeriscopeUser user, Ambari ambari) {
        CloudbreakClient client = cloudbreakService.getClient();
        try {
            return client.hasAccess(user.getId(), user.getAccount(), ambari.getHost());
        } catch (HttpResponseException e) {
            // if the cluster is unknown for cloudbreak
            // it should allow it to monitor
            return true;
        }
    }

}
