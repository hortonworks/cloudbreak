package com.sequenceiq.redbeams.flow.redbeams.rotate;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;

public class SslCertRotateDatabaseRedbeamsContext extends RedbeamsContext {

    private final boolean onlyCertificateUpdate;

    public SslCertRotateDatabaseRedbeamsContext(FlowParameters flowParameters, CloudContext cloudContext, CloudCredential cloudCredential,
            DatabaseStack databaseStack, DBStack dbStack, boolean onlyCertificateUpdate) {
        super(flowParameters, cloudContext, cloudCredential, databaseStack, dbStack);
        this.onlyCertificateUpdate = onlyCertificateUpdate;
    }

    public boolean isOnlyCertificateUpdate() {
        return onlyCertificateUpdate;
    }
}
