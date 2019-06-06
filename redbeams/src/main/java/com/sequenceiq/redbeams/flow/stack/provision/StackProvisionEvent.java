package com.sequenceiq.redbeams.flow.stack.provision;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.CreateCredentialResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.SetupResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.ValidationResult;
import com.sequenceiq.flow.core.FlowEvent;

public enum StackProvisionEvent implements FlowEvent {
    START_CREATION_EVENT("STACK_PROVISION_TRIGGER_EVENT"),
    VALIDATION_FINISHED_EVENT(CloudPlatformResult.selector(ValidationResult.class)),
    VALIDATION_FAILED_EVENT(CloudPlatformResult.failureSelector(ValidationResult.class)),
    SETUP_FINISHED_EVENT(CloudPlatformResult.selector(SetupResult.class)),
    SETUP_FAILED_EVENT(CloudPlatformResult.failureSelector(SetupResult.class)),
    CREATE_CREDENTIAL_FINISHED_EVENT(CloudPlatformResult.selector(CreateCredentialResult.class)),
    CREATE_CREDENTIAL_FAILED_EVENT(CloudPlatformResult.failureSelector(CreateCredentialResult.class)),
    LAUNCH_STACK_FINISHED_EVENT(CloudPlatformResult.selector(LaunchStackResult.class)),
    LAUNCH_STACK_FAILED_EVENT(CloudPlatformResult.failureSelector(LaunchStackResult.class)),
    STACK_CREATION_FAILED_EVENT("STACK_CREATION_FAILED"),
    STACK_CREATION_FINISHED_EVENT("STACK_CREATION_FINISHED"),
    STACKCREATION_FAILURE_HANDLED_EVENT("STACK_CREATION_FAILHANDLED");

    private final String event;

    StackProvisionEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
