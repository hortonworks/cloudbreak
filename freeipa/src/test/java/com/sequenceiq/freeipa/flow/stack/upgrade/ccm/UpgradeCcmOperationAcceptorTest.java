package com.sequenceiq.freeipa.flow.stack.upgrade.ccm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.repository.OperationRepository;
import com.sequenceiq.freeipa.service.freeipa.user.AcceptResult;

@ExtendWith(MockitoExtension.class)
class UpgradeCcmOperationAcceptorTest {

    private static final long ID_1 = 1L;

    private static final long ID_2 = 2L;

    private static final String ACCOUNT_ID = "accountId";

    private static final String ENVIRONMENT_CRN_1 = "environmentCrn1";

    private static final String ENVIRONMENT_CRN_2 = "environmentCrn2";

    @Mock
    private OperationRepository operationRepository;

    @InjectMocks
    private UpgradeCcmOperationAcceptor underTest;

    @Test
    void selectorTest() {
        assertThat(underTest.selector()).isEqualTo(OperationType.UPGRADE_CCM);
    }

    static Object[][] acceptTestWhenValidationFailureDataProvider() {
        return new Object[][]{
                // testCaseName environmentList
                {"environmentList=null", null},
                {"environmentList=()", List.of()},
                {"environmentList=(ENVIRONMENT_CRN_1, ENVIRONMENT_CRN_2)", List.of(ENVIRONMENT_CRN_1, ENVIRONMENT_CRN_2)},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("acceptTestWhenValidationFailureDataProvider")
    void acceptTestWhenValidationFailure(String testCaseName, List<String> environmentList) {
        Operation operation = createOperation(ID_1, environmentList);

        AcceptResult result = underTest.accept(operation);

        assertThat(result).isNotNull();
        assertThat(result.isAccepted()).isFalse();

        Optional<String> rejectionMessage = result.getRejectionMessage();
        assertThat(rejectionMessage).isNotNull();
        assertThat(rejectionMessage).isPresent();
        assertThat(rejectionMessage.get()).isEqualTo("Cluster Connectivity Manager upgrade must be invoked for a single environment!");
    }

    @Test
    void acceptTestWhenHasRunningOperationForSameStack() {
        Operation operation = createOperation(ID_1, List.of(ENVIRONMENT_CRN_1));
        when(operationRepository.findRunningByAccountIdAndType(ACCOUNT_ID, OperationType.UPGRADE_CCM))
                .thenReturn(List.of(createOperation(ID_2, List.of(ENVIRONMENT_CRN_1))));

        AcceptResult result = underTest.accept(operation);

        assertThat(result).isNotNull();
        assertThat(result.isAccepted()).isFalse();

        Optional<String> rejectionMessage = result.getRejectionMessage();
        assertThat(rejectionMessage).isNotNull();
        assertThat(rejectionMessage).isPresent();
        assertThat(rejectionMessage.get()).isEqualTo("There is already a running Cluster Connectivity Manager upgrade for FreeIPA stack");
    }

    static Object[][] acceptTestWhenSuccessDataProvider() {
        return new Object[][]{
                // testCaseName runningOperations
                {"runningOperations=()", List.of()},
                {"runningOperations=((ID_1, ENVIRONMENT_CRN_1))", List.of(createOperation(ID_1, List.of(ENVIRONMENT_CRN_1)))},
                {"runningOperations=((ID_2, ENVIRONMENT_CRN_2))", List.of(createOperation(ID_2, List.of(ENVIRONMENT_CRN_2)))},
                {"runningOperations=((ID_1, ENVIRONMENT_CRN_1), (ID_2, ENVIRONMENT_CRN_2))", List.of(createOperation(ID_1, List.of(ENVIRONMENT_CRN_1)),
                        createOperation(ID_2, List.of(ENVIRONMENT_CRN_2)))},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("acceptTestWhenSuccessDataProvider")
    void acceptTestWhenSuccess(String testCaseName, List<Operation> runningOperations) {
        Operation operation = createOperation(ID_1, List.of(ENVIRONMENT_CRN_1));
        when(operationRepository.findRunningByAccountIdAndType(ACCOUNT_ID, OperationType.UPGRADE_CCM)).thenReturn(runningOperations);

        AcceptResult result = underTest.accept(operation);

        assertThat(result).isNotNull();
        assertThat(result.isAccepted()).isTrue();
    }

    private static Operation createOperation(long id, List<String> environmentList) {
        Operation operation = new Operation();
        operation.setId(id);
        operation.setAccountId(ACCOUNT_ID);
        operation.setEnvironmentList(environmentList);
        return operation;
    }

}