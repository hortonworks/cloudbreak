package com.sequenceiq.environment.encryptionprofile.service;

import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.ACCOUNT_ID;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.CREATOR;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.CRN;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.NAME;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Supplier;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;

import org.hibernate.JDBCException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.encryptionprofile.respository.EncryptionProfileRepository;

@ExtendWith(MockitoExtension.class)
public class EncryptionProfileServiceTest {

    private static final EncryptionProfile ENCRYPTION_PROFILE = EncryptionProfileTestConstants.getTestEncryptionProfile();

    @Mock
    private EncryptionProfileRepository repository;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Mock
    private OwnerAssignmentService ownerAssignmentService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private EncryptionProfileService underTest;

    @BeforeEach
    void setUp() throws TransactionService.TransactionExecutionException {
        lenient().doNothing().when(ownerAssignmentService).assignResourceOwnerRoleIfEntitled(any(), any());
        lenient().doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get())
                .when(transactionService).required(any(Supplier.class));
        lenient().when(regionAwareCrnGenerator.generateCrnStringWithUuid(any(), eq(ACCOUNT_ID))).thenReturn(CRN);
        when(entitlementService.isConfigureEncryptionProfileEnabled(ACCOUNT_ID)).thenReturn(true);
    }

    @Test
    void testCreateSuccessful() throws TransactionService.TransactionExecutionException {
        when(repository.findByNameAndAccountId(NAME, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(ENCRYPTION_PROFILE);

        EncryptionProfile result = underTest.create(ENCRYPTION_PROFILE, ACCOUNT_ID, CREATOR);

        assertNotNull(result);
        assertEquals(CRN, result.getResourceCrn());
        assertEquals(ACCOUNT_ID, result.getAccountId());

        verify(ownerAssignmentService).assignResourceOwnerRoleIfEntitled(CREATOR, CRN);
        verify(transactionService).required(any(Supplier.class));
        verify(repository).save(ENCRYPTION_PROFILE);
    }

    @Test
    void testCreateWithoutEntitlement() {
        when(entitlementService.isConfigureEncryptionProfileEnabled(ACCOUNT_ID)).thenReturn(false);
        assertThatThrownBy(() -> underTest.create(ENCRYPTION_PROFILE, ACCOUNT_ID, CREATOR))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Encryption profile creation is not enabled for account: " + ACCOUNT_ID);
    }

    @Test
    void testCreateFailsIfAlreadyExists() {
        when(repository.findByNameAndAccountId(NAME, ACCOUNT_ID))
                .thenReturn(Optional.of(ENCRYPTION_PROFILE));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> underTest.create(ENCRYPTION_PROFILE, ACCOUNT_ID, CREATOR));

        assertEquals("Encryption Profile already exists with name: " + NAME, ex.getMessage());

        verify(repository, never()).save(any());
        verify(ownerAssignmentService, never()).assignResourceOwnerRoleIfEntitled(any(), any());
    }

    @Test
    void testCreateFailsWithTransactionException() throws TransactionService.TransactionExecutionException {
        when(repository.findByNameAndAccountId(NAME, ACCOUNT_ID)).thenReturn(Optional.empty());

        // Simulate JDBCException being thrown from inside the Supplier
        JDBCException jdbcException = new JDBCException("DB error", null);
        TransactionService.TransactionExecutionException wrapperException =
                new TransactionService.TransactionExecutionException("DB error", jdbcException);

        when(transactionService.required(any(Supplier.class))).thenThrow(wrapperException);

        InternalServerErrorException thrown = assertThrows(InternalServerErrorException.class,
                () -> underTest.create(ENCRYPTION_PROFILE, ACCOUNT_ID, CREATOR));

        assertEquals("HTTP 500 Internal Server Error", thrown.getMessage());
        assertNotNull(thrown.getCause());
        assertEquals("DB error", thrown.getCause().getMessage());

        verify(ownerAssignmentService).notifyResourceDeleted(CRN);
    }
}
