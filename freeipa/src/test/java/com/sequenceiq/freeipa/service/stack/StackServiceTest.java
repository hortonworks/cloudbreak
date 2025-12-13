package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.dal.ResourceBasicView;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.api.telemetry.model.Monitoring;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.StackRepository;

@ExtendWith(MockitoExtension.class)
class StackServiceTest {

    private static final String ENVIRONMENT_CRN = "test:environment:crn";

    private static final String CHILD_ENVIRONMENT_CRN = "test:environment:child-crn";

    private static final String FREEIPA_CRN = "test:freeipa:crn";

    private static final Long STACK_ID = 1L;

    private static final String STACK_NAME = "stack-name";

    private static final String ACCOUNT_ID = "account:id";

    private ResourceBasicView resourceBasicView = new ResourceBasicView() {
        @Override
        public Long getId() {
            return null;
        }

        @Override
        public String getResourceCrn() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getEnvironmentCrn() {
            return null;
        }
    };

    @InjectMocks
    private StackService underTest;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private ChildEnvironmentService childEnvironmentService;

    private Stack stack;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName(STACK_NAME);
    }

    @Test
    void getByIdWithListsInTransactionNotFound() {
        when(stackRepository.findOneWithLists(STACK_ID)).thenReturn(Optional.empty());
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> underTest.getByIdWithListsInTransaction(STACK_ID));
        assertEquals("FreeIPA stack [" + STACK_ID + "] not found", notFoundException.getMessage());
    }

    @Test
    void getByIdWithListsInTransaction() {
        when(stackRepository.findOneWithLists(STACK_ID)).thenReturn(Optional.of(stack));
        Stack stackByIdWithListsInTransaction = underTest.getByIdWithListsInTransaction(STACK_ID);
        assertEquals(stack, stackByIdWithListsInTransaction);
    }

    @Test
    void getResourceBasicViewByCrnNotFound() {
        when(stackRepository.findResourceBasicViewByResourceCrn(FREEIPA_CRN)).thenReturn(Optional.empty());
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> underTest.getResourceBasicViewByCrn(FREEIPA_CRN));
        assertEquals("FreeIPA stack [" + FREEIPA_CRN + "] not found", notFoundException.getMessage());
    }

    @Test
    void getResourceBasicViewByCrn() {
        when(stackRepository.findResourceBasicViewByResourceCrn(FREEIPA_CRN)).thenReturn(Optional.of(resourceBasicView));
        ResourceBasicView resourceBasicView1 = underTest.getResourceBasicViewByCrn(FREEIPA_CRN);
        assertEquals(resourceBasicView, resourceBasicView1);
    }

    @Test
    void getStackByIdNotFound() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.empty());
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> underTest.getStackById(STACK_ID));
        assertEquals("FreeIPA stack [" + STACK_ID + "] not found", notFoundException.getMessage());
    }

    @Test
    void getStackById() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(stack));
        Stack stackById = underTest.getStackById(STACK_ID);
        assertEquals(stack, stackById);
    }

    @Test
    void save() {
        when(stackRepository.save(stack)).thenReturn(stack);
        Stack savedStack = underTest.save(stack);
        assertEquals(stack, savedStack);
    }

    @Test
    void getByEnvironmentCrnNotFound() {
        when(stackRepository.findByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(childEnvironmentService.findParentByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.empty());
        NotFoundException notFoundException =
                assertThrows(NotFoundException.class, () -> underTest.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID));
        assertEquals("FreeIPA stack by environment [" + ENVIRONMENT_CRN + "] not found", notFoundException.getMessage());
    }

    @Test
    void getByEnvironmentCrnWhenOnlyParentsStackFound() {
        when(stackRepository.findByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(childEnvironmentService.findParentByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.of(stack));
        Stack stackByEnvironmentCrn = underTest.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID);
        assertEquals(stack, stackByEnvironmentCrn);
    }

    @Test
    void getByEnvironmentCrn() {
        when(stackRepository.findByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.of(stack));
        Stack stackByEnvironmentCrn = underTest.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID);
        assertEquals(stack, stackByEnvironmentCrn);
    }

    @Test
    void getByEnvironmentCrnAndAccountIdWithListsShouldReturnByEnvironmentCrn() {
        when(stackRepository.findByEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.of(stack));

        Stack result = underTest.getByEnvironmentCrnAndAccountIdWithLists(ENVIRONMENT_CRN, ACCOUNT_ID);

        verify(stackRepository).findByEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID);
        verify(stackRepository, never()).findByChildEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID);
        assertEquals(stack, result);
    }

    @Test
    void getByEnvironmentCrnAndAccountIdWithListsShouldReturnByChildEnvironmentCrn() {
        when(stackRepository.findByEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(stackRepository.findByChildEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.of(stack));

        Stack result = underTest.getByEnvironmentCrnAndAccountIdWithLists(ENVIRONMENT_CRN, ACCOUNT_ID);

        verify(stackRepository).findByEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID);
        verify(stackRepository).findByChildEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID);
        assertEquals(stack, result);
    }

    @Test
    void getByEnvironmentCrnAndAccountIdWithListsShouldFailAsFallback() {
        when(stackRepository.findByEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(stackRepository.findByChildEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> underTest.getByEnvironmentCrnAndAccountIdWithLists(ENVIRONMENT_CRN, ACCOUNT_ID));
        verify(stackRepository).findByEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID);
        verify(stackRepository).findByChildEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID);
    }

    @Test
    void findAllWithDetailedStackStatuses() {
        when(stackRepository.findAllWithDetailedStackStatuses(eq(List.of(DetailedStackStatus.AVAILABLE)))).thenReturn(List.of(stack));
        List<Stack> results = underTest.findAllWithDetailedStackStatuses(List.of(DetailedStackStatus.AVAILABLE));

        assertEquals(List.of(stack), results);
    }

    @Test
    void findByCrnAndAccountIdWithListsEvenIfTerminated() {
        when(stackRepository.findByAccountIdEnvironmentCrnAndCrnWithListsEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID, FREEIPA_CRN))
                .thenReturn(Optional.of(stack));
        Optional<Stack> results = underTest.findByCrnAndAccountIdWithListsEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID, FREEIPA_CRN);

        assertEquals(Optional.of(stack), results);
    }

    @Test
    void getByCrnAndAccountIdEvenIfTerminated() {
        when(stackRepository.findByAccountIdEnvironmentCrnAndCrnWithListsEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID, FREEIPA_CRN))
                .thenReturn(Optional.of(stack));
        Stack results = underTest.getByCrnAndAccountIdEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID, FREEIPA_CRN);

        assertEquals(stack, results);
    }

    @Test
    void getByCrnAndAccountIdEvenIfTerminatedThrowsWhenNotPresent() {
        when(stackRepository.findByAccountIdEnvironmentCrnAndCrnWithListsEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID, FREEIPA_CRN))
                .thenReturn(Optional.empty());
        when(childEnvironmentService.findParentStackByChildEnvironmentCrnAndCrnWithListsEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID, FREEIPA_CRN))
                .thenReturn(Optional.empty());
        NotFoundException notFoundException = assertThrows(NotFoundException.class,
                () -> underTest.getByCrnAndAccountIdEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID, FREEIPA_CRN));
        assertEquals("FreeIPA stack by environment [" + ENVIRONMENT_CRN + "] with CRN [" + FREEIPA_CRN + "] not found", notFoundException.getMessage());
    }

    @Test
    void findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminatedWithList() {
        when(stackRepository.findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminatedWithList(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(List.of(stack));
        List<Stack> results = underTest.findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminatedWithList(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertEquals(List.of(stack), results);
    }

    @Test
    void findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminated() {
        when(stackRepository.findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(List.of(stack));
        List<Stack> results = underTest.findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertEquals(List.of(stack), results);
    }

    @Test
    void testGetComputeMonitoringFlagWhenEnabled() {
        Optional<Boolean> computeMonitoringEnabled = underTest.computeMonitoringEnabled(stackWithTelemetry(true));
        assertTrue(computeMonitoringEnabled.isPresent());
        assertTrue(computeMonitoringEnabled.get());
    }

    @Test
    void testGetComputeMonitoringFlagWhenDisabled() {
        Optional<Boolean> computeMonitoringEnabled = underTest.computeMonitoringEnabled(stackWithTelemetry(false));
        assertTrue(computeMonitoringEnabled.isPresent());
        assertFalse(computeMonitoringEnabled.get());
    }

    @Test
    void testGetComputeMonitoringFlagWhenTelemetryNull() {
        Optional<Boolean> computeMonitoringEnabled = underTest.computeMonitoringEnabled(new Stack());
        assertFalse(computeMonitoringEnabled.isPresent());
    }

    private Stack stackWithTelemetry(boolean monitoringEnabled) {
        Telemetry telemetry = new Telemetry();
        if (monitoringEnabled) {
            Monitoring monitoring = new Monitoring();
            monitoring.setRemoteWriteUrl("something");
            telemetry.setMonitoring(monitoring);
        }
        Stack stack = new Stack();
        stack.setTelemetry(telemetry);
        return stack;
    }
}
