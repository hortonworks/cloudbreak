package com.sequenceiq.redbeams.flow.redbeams.stop;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class RedbeamsStopContext extends CommonContext {

    private final CloudContext cloudContext;

    private final CloudCredential cloudCredential;

    private final String dbInstanceIdentifier;

    private final String dbVendorDisplayName;

    public RedbeamsStopContext(FlowParameters flowParameters,
                                CloudContext cloudContext,
                                CloudCredential cloudCredential,
                                String dbInstanceIdentifier,
                                String dbVendorDisplayName) {
        super(flowParameters);
        this.cloudContext = cloudContext;
        this.cloudCredential = cloudCredential;
        this.dbInstanceIdentifier = dbInstanceIdentifier;
        this.dbVendorDisplayName = dbVendorDisplayName;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public String getDbInstanceIdentifier() {
        return dbInstanceIdentifier;
    }

    public String getDbVendorDisplayName() {
        return dbVendorDisplayName;
    }
}
