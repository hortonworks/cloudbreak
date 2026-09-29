package com.sequenceiq.cloudbreak.core.cluster.prerequisite;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.StackDto;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerUpgradePrerequisiteServiceTest {

    @Mock
    private ClouderaManagerUpgradePrerequisite firstPrerequisite;

    @Mock
    private ClouderaManagerUpgradePrerequisite secondPrerequisite;

    @Mock
    private StackDto stack;

    @InjectMocks
    private ClouderaManagerUpgradePrerequisiteService underTest;

    @Test
    void shouldExecuteEveryPrerequisiteInOrder() {
        setPrerequisites(List.of(firstPrerequisite, secondPrerequisite));

        underTest.executePrerequisites(stack);

        InOrder inOrder = inOrder(firstPrerequisite, secondPrerequisite);
        inOrder.verify(firstPrerequisite).execute(stack);
        inOrder.verify(secondPrerequisite).execute(stack);
    }

    @Test
    void shouldPropagateTheFailureAndSkipTheRemainingPrerequisites() {
        setPrerequisites(List.of(firstPrerequisite, secondPrerequisite));
        doThrow(new CloudbreakServiceException("prerequisite failed")).when(firstPrerequisite).execute(stack);

        assertThrows(CloudbreakServiceException.class, () -> underTest.executePrerequisites(stack));

        verify(firstPrerequisite).execute(stack);
        verifyNoInteractions(secondPrerequisite);
    }

    @Test
    void shouldDoNothingWhenThereIsNoPrerequisite() {
        setPrerequisites(List.of());

        assertDoesNotThrow(() -> underTest.executePrerequisites(stack));
    }

    @Test
    void shouldDoNothingWhenThePrerequisitesAreNotInjected() {
        setPrerequisites(null);

        assertDoesNotThrow(() -> underTest.executePrerequisites(stack));
    }

    private void setPrerequisites(List<ClouderaManagerUpgradePrerequisite> prerequisites) {
        ReflectionTestUtils.setField(underTest, "prerequisites", prerequisites);
    }
}
