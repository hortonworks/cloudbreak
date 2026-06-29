package com.sequenceiq.freeipa.flow.freeipa.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.azure.AzureConstants;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpConstants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;

class MultiAzMigrationEventTest {

    private static final Long STACK_ID = 123L;

    private static final String OPERATION_ID = "operationId";

    private static final String PRIMARY_GW = "i-pgw";

    private static final String NON_PGW_1 = "i-non-pgw-1";

    private static final String NON_PGW_2 = "i-non-pgw-2";

    static Stream<Arguments> variantArguments() {
        return Stream.of(
                Arguments.of(null, null, false),
                Arguments.of(null, AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant(), false),
                Arguments.of(AwsConstants.AwsVariant.AWS_VARIANT.variant(), null, false),
                Arguments.of(AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant(), AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant(), false),
                Arguments.of(AwsConstants.AwsVariant.AWS_VARIANT.variant(), AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant(), true),
                Arguments.of(AzureConstants.VARIANT, AzureConstants.VARIANT, false),
                Arguments.of(GcpConstants.GCP_VARIANT, GcpConstants.GCP_VARIANT, false)
        );
    }

    @MethodSource("variantArguments")
    @ParameterizedTest
    void testVariantMigrationNeeded(Variant sourceVariant, Variant targetVariant, boolean expected) {
        MultiAzMigrationEvent underTest = getMultiAzMigrationEvent(sourceVariant, targetVariant);
        assertEquals(expected, underTest.variantMigrationNeeded());
    }

    @MethodSource("variantArguments")
    @ParameterizedTest
    void testShouldRecreatePrimaryGw(Variant sourceVariant, Variant targetVariant, boolean expected) {
        MultiAzMigrationEvent underTest = getMultiAzMigrationEvent(sourceVariant, targetVariant);
        assertEquals(expected, underTest.shouldRecreatePrimaryGw());
    }

    @Test
    void testgetNonPrimaryGwInstanceIdsToRecreate() {
        MultiAzMigrationEvent underTest = getMultiAzMigrationEvent();

        Set<String> result = underTest.getNonPrimaryGwInstanceIdsToRecreate();

        assertThat(result).containsExactlyInAnyOrder(NON_PGW_1, NON_PGW_2);
    }

    private static MultiAzMigrationEvent getMultiAzMigrationEvent() {
        return getMultiAzMigrationEvent(AwsConstants.AwsVariant.AWS_VARIANT.variant(), AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant());
    }

    private static MultiAzMigrationEvent getMultiAzMigrationEvent(Variant sourceVariant, Variant targetVariant) {
        return new MultiAzMigrationEvent(
                FlowChainTriggers.MULTI_AZ_MIGRATION_TRIGGER_EVENT,
                STACK_ID,
                OPERATION_ID,
                sourceVariant,
                targetVariant,
                new HashSet<>(Set.of(PRIMARY_GW, NON_PGW_1, NON_PGW_2)),
                PRIMARY_GW
        );
    }
}
