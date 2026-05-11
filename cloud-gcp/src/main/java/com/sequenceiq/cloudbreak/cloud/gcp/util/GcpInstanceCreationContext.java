package com.sequenceiq.cloudbreak.cloud.gcp.util;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;

public record GcpInstanceCreationContext(GcpContext context, AuthenticatedContext auth) {
}
