package com.sequenceiq.cloudbreak.core.flow2.cluster.trustedrealm;

import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class UpdateTrustedRealmContext extends CommonContext {

    private final StackView stack;

    private final String environmentCrn;

    private final String realm;

    private final boolean remove;

    public UpdateTrustedRealmContext(FlowParameters flowParameters, StackView stack, String environmentCrn, String realm, boolean remove) {
        super(flowParameters);
        this.stack = stack;
        this.environmentCrn = environmentCrn;
        this.realm = realm;
        this.remove = remove;
    }

    public StackView getStack() {
        return stack;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public String getRealm() {
        return realm;
    }

    public boolean isRemove() {
        return remove;
    }
}
