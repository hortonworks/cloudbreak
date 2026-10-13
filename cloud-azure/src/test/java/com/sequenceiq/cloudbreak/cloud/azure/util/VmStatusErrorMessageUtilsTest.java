package com.sequenceiq.cloudbreak.cloud.azure.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

import reactor.core.Exceptions;

class VmStatusErrorMessageUtilsTest {

    @Test
    void createErrorMessageShouldOmitErrorMessagesWhenStatusesCarryTheReasons() {
        String errorMessage = VmStatusErrorMessageUtils.createErrorMessage("prefix: ", new RuntimeException("failed"), List.of(
                new CloudVmInstanceStatus(createCloudInstance("instance1"), InstanceStatus.STOPPED),
                new CloudVmInstanceStatus(createCloudInstance("instance2"), InstanceStatus.FAILED, "failed")));

        assertThat(errorMessage).isEqualTo("prefix: statuses: [instance1 - STOPPED, instance2 - FAILED - failed]");
    }

    @Test
    void createErrorMessageShouldIncludeErrorMessagesWhenStatusesHaveNoReasons() {
        String errorMessage = VmStatusErrorMessageUtils.createErrorMessage("prefix: ", new RuntimeException("sync error"), List.of(
                new CloudVmInstanceStatus(createCloudInstance("instance1"), InstanceStatus.STOPPED)));

        assertThat(errorMessage).isEqualTo("prefix: sync error, statuses: [instance1 - STOPPED]");
    }

    @Test
    void createErrorMessageShouldIncludeErrorMessagesWhenStatusesAreEmpty() {
        String errorMessage = VmStatusErrorMessageUtils.createErrorMessage("prefix: ", new RuntimeException("sync error"), List.of());

        assertThat(errorMessage).isEqualTo("prefix: sync error, statuses: []");
    }

    @Test
    void createErrorMessageShouldIncludeErrorMessageWhenOtherStatusCarriesUnrelatedReason() {
        String errorMessage = VmStatusErrorMessageUtils.createErrorMessage("prefix: ", new RuntimeException("sync error"), List.of(
                new CloudVmInstanceStatus(createCloudInstance("instance1"), InstanceStatus.UNKNOWN, "vm not found")));

        assertThat(errorMessage).isEqualTo("prefix: sync error, statuses: [instance1 - UNKNOWN - vm not found]");
    }

    @Test
    void createErrorMessageShouldIncludeOnlySuppressedMessagesMissingFromStatuses() {
        RuntimeException composite = new RuntimeException("composite");
        composite.addSuppressed(new RuntimeException("failed"));
        composite.addSuppressed(new RuntimeException("not subscribed"));
        Exception multiple = (Exception) Exceptions.multiple(composite.getSuppressed());

        String errorMessage = VmStatusErrorMessageUtils.createErrorMessage("prefix: ", multiple, List.of(
                new CloudVmInstanceStatus(createCloudInstance("instance1"), InstanceStatus.FAILED, "failed")));

        assertThat(errorMessage).isEqualTo("prefix: not subscribed, statuses: [instance1 - FAILED - failed]");
    }

    @Test
    void convertToStatusTextShouldSeparateStatusesAndIncludeReasonWhenPresent() {
        String statusText = VmStatusErrorMessageUtils.convertToStatusText(List.of(
                new CloudVmInstanceStatus(createCloudInstance("instance1"), InstanceStatus.STOPPED),
                new CloudVmInstanceStatus(createCloudInstance("instance2"), InstanceStatus.FAILED, "failed")));

        assertThat(statusText).isEqualTo("[instance1 - STOPPED, instance2 - FAILED - failed]");
    }

    @Test
    void convertToStatusTextShouldHandleEmptyStatuses() {
        assertThat(VmStatusErrorMessageUtils.convertToStatusText(List.of())).isEqualTo("[]");
    }

    private CloudInstance createCloudInstance(String instanceId) {
        return new CloudInstance(instanceId, null, null, "subnet-1", "az1");
    }
}
