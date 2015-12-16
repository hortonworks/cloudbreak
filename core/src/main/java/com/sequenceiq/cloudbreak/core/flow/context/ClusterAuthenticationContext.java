package com.sequenceiq.cloudbreak.core.flow.context;


import com.sequenceiq.cloudbreak.cloud.model.Platform;

public class ClusterAuthenticationContext extends DefaultFlowContext {

    private String user;
    private String password;

    public ClusterAuthenticationContext(Long stackId, Platform cp, String user, String password) {
        super(stackId, cp);
        this.user = user;
        this.password = password;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }
}
