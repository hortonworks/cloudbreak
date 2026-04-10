package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeReinitiableV4Response;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.reinit.UpgradeReinitiateStatus;

@ExtendWith(MockitoExtension.class)
class UpgradeReinitiateServiceTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private StackStatusService stackStatusService;

    @InjectMocks
    private UpgradeReinitiateService underTest;

    static Stream<Arguments> testCheckClusterUpgradeReinitiableArguments() {
        Stream.Builder<Arguments> arguments = Stream.builder();
        arguments.add(Arguments.of(List.of(
                        getStackStatus(DetailedStackStatus.AVAILABLE)
                ),
                UpgradeReinitiateStatus.NON_REINITIABLE,
                "There were no upgrades for this cluster, therefore upgrade reinitiation is not needed."
        ));
        for (DetailedStackStatus successfulUpgradeStatus : DetailedStackStatus.getUpgradeSuccessStatuses()) {
            arguments.add(Arguments.of(List.of(
                            getStackStatus(DetailedStackStatus.AVAILABLE),
                            getStackStatus(successfulUpgradeStatus),
                            getStackStatus(DetailedStackStatus.AVAILABLE)
                    ),
                    UpgradeReinitiateStatus.NON_REINITIABLE,
                    "The last upgrade for this cluster finished successfully, therefore upgrade reinitiation is not needed."
            ));
        }
        for (DetailedStackStatus failedUpgradeStatus : DetailedStackStatus.getUpgradeFailureStatuses()) {
            arguments.add(Arguments.of(List.of(
                            getStackStatus(DetailedStackStatus.AVAILABLE),
                            getStackStatus(failedUpgradeStatus),
                            getStackStatus(DetailedStackStatus.AVAILABLE)
                    ),
                    UpgradeReinitiateStatus.REINITIABLE,
                    "The last upgrade for this cluster finished with a failure, therefore the cluster is eligible for upgrade reinitiation."
            ));
        }
        return arguments.build();
    }

    @MethodSource("testCheckClusterUpgradeReinitiableArguments")
    @ParameterizedTest
    void testCheckClusterUpgradeReinitiable(List<StackStatus> stackStatuses, UpgradeReinitiateStatus expectedStatus, String expectedReason) {
        when(stackStatusService.findAllStackStatusesById(STACK_ID)).thenReturn(stackStatuses);

        UpgradeReinitiableV4Response result = underTest.checkClusterUpgradeReinitiable(STACK_ID);

        assertEquals(expectedStatus, result.status());
        assertEquals(expectedReason, result.reason());
    }

    private static StackStatus getStackStatus(DetailedStackStatus detailedStackStatus) {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setDetailedStackStatus(detailedStackStatus);
        return stackStatus;
    }
}
