package com.sequenceiq.cloudbreak.core.flow2.stack.migration;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.CreateResourcesResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.DeleteCloudFormationResult;
import com.sequenceiq.flow.core.FlowEvent;

public enum AwsVariantMigrationEvent implements FlowEvent {
    CREATE_RESOURCES_EVENT("CREATE_RESOURCES_EVENT"),
    DELETE_CLOUD_FORMATION_EVENT(CloudPlatformResult.selector(CreateResourcesResult.class)),
    CHANGE_VARIANT_EVENT(CloudPlatformResult.selector(DeleteCloudFormationResult.class)),
    AWS_VARIANT_MIGRATION_FINALIZED_EVENT("AWSVARIANTMIGRATIONFINALIZEDEVENT"),
    AWS_VARIANT_MIGRATION_FAILED_EVENT("AWSVARIANTMIGRATIONFAILEDEVENT"),
    AWS_VARIANT_MIGRATION_FAIL_HANDLED_EVENT("AWSVARIANTMIGRATIONFINALIZED_EVENT");

    private final String event;

    AwsVariantMigrationEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
