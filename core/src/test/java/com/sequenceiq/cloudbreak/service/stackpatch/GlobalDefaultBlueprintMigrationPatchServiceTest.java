package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@ExtendWith(MockitoExtension.class)
class GlobalDefaultBlueprintMigrationPatchServiceTest {

    private static final Long CLUSTER_ID = 10L;

    private static final String STACK_CRN = "crn:cdp:datahub:us-west-1:account-id:cluster:uuid";

    private static final String BLUEPRINT_NAME = "CDP 1.0";

    private static final String ACCOUNT_BLUEPRINT_CRN = "crn:cdp:datahub:us-west-1:account-id:clustertemplate:workspace-specific";

    private static final String GLOBAL_DEFAULT_BLUEPRINT_CRN = "crn:cdp:datahub:us-west-1:cloudera_default:clustertemplate:cdp10";

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private ClusterService clusterService;

    @InjectMocks
    private GlobalDefaultBlueprintMigrationPatchService underTest;

    @Test
    void getStackPatchTypeShouldReturnGlobalDefaultBlueprintMigration() {
        assertThat(underTest.getStackPatchType()).isEqualTo(StackPatchType.GLOBAL_DEFAULT_BLUEPRINT_MIGRATION);
    }

    @Test
    void isAffectedShouldReturnTrueWhenBlueprintCrnIsNotYetGlobalDefault() {
        Stack stack = stackWithBlueprint(blueprint(BLUEPRINT_NAME, ACCOUNT_BLUEPRINT_CRN, ResourceStatus.DEFAULT));

        assertThat(underTest.isAffected(stack)).isTrue();
        verify(blueprintService, never()).getGlobalDefaultBlueprintByName(any());
    }

    @Test
    void isAffectedShouldReturnFalseWhenBlueprintIsNull() {
        Stack stack = stackWithBlueprint(null);

        assertThat(underTest.isAffected(stack)).isFalse();
    }

    @Test
    void isAffectedShouldReturnFalseWhenBlueprintCrnIsNull() {
        Stack stack = stackWithBlueprint(blueprint(BLUEPRINT_NAME, null, ResourceStatus.DEFAULT));

        assertThat(underTest.isAffected(stack)).isFalse();
    }

    @Test
    void isAffectedShouldReturnFalseWhenBlueprintStatusIsNull() {
        Stack stack = stackWithBlueprint(blueprint(BLUEPRINT_NAME, ACCOUNT_BLUEPRINT_CRN, null));

        assertThat(underTest.isAffected(stack)).isFalse();
    }

    @Test
    void isAffectedShouldReturnFalseWhenBlueprintIsNonDefault() {
        Stack stack = stackWithBlueprint(blueprint(BLUEPRINT_NAME, ACCOUNT_BLUEPRINT_CRN, ResourceStatus.USER_MANAGED));

        assertThat(underTest.isAffected(stack)).isFalse();
    }

    @Test
    void isAffectedShouldReturnFalseWhenBlueprintCrnIsAlreadyGlobalDefault() {
        Stack stack = stackWithBlueprint(blueprint(BLUEPRINT_NAME, GLOBAL_DEFAULT_BLUEPRINT_CRN, ResourceStatus.DEFAULT));

        assertThat(underTest.isAffected(stack)).isFalse();
        verify(blueprintService, never()).getGlobalDefaultBlueprintByName(any());
    }

    @Test
    void doApplyShouldMigrateBlueprintAndReturnTrue() throws ExistingStackPatchApplyException {
        Stack stack = stackWithBlueprint(blueprint(BLUEPRINT_NAME, ACCOUNT_BLUEPRINT_CRN, ResourceStatus.DEFAULT));
        Cluster cluster = mock(Cluster.class);
        when(cluster.getId()).thenReturn(CLUSTER_ID);
        when(stack.getCluster()).thenReturn(cluster);
        Blueprint globalDefaultBlueprint = blueprint(BLUEPRINT_NAME, GLOBAL_DEFAULT_BLUEPRINT_CRN, ResourceStatus.DEFAULT);
        when(blueprintService.getGlobalDefaultBlueprintByName(BLUEPRINT_NAME)).thenReturn(Optional.of(globalDefaultBlueprint));

        assertThat(underTest.doApply(stack)).isTrue();
        verify(clusterService, times(1)).updateBlueprint(CLUSTER_ID, globalDefaultBlueprint);
    }

    @Test
    void doApplyShouldNotMigrateButReturnTrueWhenGlobalDefaultBlueprintIsNotDefaultAnymore() throws ExistingStackPatchApplyException {
        Stack stack = stackWithBlueprint(blueprint(BLUEPRINT_NAME, ACCOUNT_BLUEPRINT_CRN, ResourceStatus.DEFAULT));
        Blueprint globalDefaultBlueprint = blueprint(BLUEPRINT_NAME, GLOBAL_DEFAULT_BLUEPRINT_CRN, ResourceStatus.DEFAULT_DELETED);
        when(blueprintService.getGlobalDefaultBlueprintByName(BLUEPRINT_NAME)).thenReturn(Optional.of(globalDefaultBlueprint));

        assertThat(underTest.doApply(stack)).isTrue();
        verify(clusterService, never()).updateBlueprint(any(), any());
    }

    @Test
    void doApplyShouldReturnFalseWhenGlobalDefaultBlueprintDisappeared() throws ExistingStackPatchApplyException {
        Stack stack = stackWithBlueprint(blueprint(BLUEPRINT_NAME, ACCOUNT_BLUEPRINT_CRN, ResourceStatus.DEFAULT));
        when(blueprintService.getGlobalDefaultBlueprintByName(BLUEPRINT_NAME)).thenReturn(Optional.empty());

        assertThat(underTest.doApply(stack)).isFalse();
        verify(clusterService, never()).updateBlueprint(any(), any());
    }

    @Test
    void shouldCheckForFailedRetryableFlowShouldReturnFalse() {
        assertThat(underTest.shouldCheckForFailedRetryableFlow()).isFalse();
    }

    private Blueprint blueprint(String name, String resourceCrn, ResourceStatus status) {
        Blueprint blueprint = mock(Blueprint.class);
        lenient().when(blueprint.getName()).thenReturn(name);
        lenient().when(blueprint.getResourceCrn()).thenReturn(resourceCrn);
        lenient().when(blueprint.getStatus()).thenReturn(status);
        return blueprint;
    }

    private Stack stackWithBlueprint(Blueprint blueprint) {
        Stack stack = mock(Stack.class);
        lenient().when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        lenient().when(stack.getBlueprint()).thenReturn(blueprint);
        return stack;
    }
}
