package com.sequenceiq.cloudbreak.domain.stack;

public enum StackPatchType {
    UNBOUND_RESTART,
    LOGGING_AGENT_AUTO_RESTART,
    LOGGING_AGENT_AUTO_RESTART_V2,
    METERING_AZURE_METADATA,
    MOCK,
    UNKNOWN
}
