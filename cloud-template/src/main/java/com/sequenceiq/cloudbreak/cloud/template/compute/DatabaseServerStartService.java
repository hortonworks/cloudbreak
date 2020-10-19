package com.sequenceiq.cloudbreak.cloud.template.compute;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;

public interface DatabaseServerStartService {

    void start(AuthenticatedContext ac, DatabaseStack stack) throws Exception;
}
