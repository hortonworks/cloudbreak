package com.sequenceiq.cloudbreak.cloud.template.compute;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.template.compute.CloudFailureHandler.ScaleContext;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;

public class CloudFailureContext {

    private AuthenticatedContext auth;

    private ScaleContext stx;

    private ResourceBuilderContext ctx;

    public CloudFailureContext(AuthenticatedContext auth, ScaleContext stx, ResourceBuilderContext ctx) {
        this.auth = auth;
        this.stx = stx;
        this.ctx = ctx;
    }

    public AuthenticatedContext getAuth() {
        return auth;
    }

    public ScaleContext getStx() {
        return stx;
    }

    public ResourceBuilderContext getCtx() {
        return ctx;
    }

}
