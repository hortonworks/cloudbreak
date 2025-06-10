package com.sequenceiq.environment.encryptionprofile.service;

import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.ACCOUNT_ID;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.CREATOR;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.ENCRYPTION_PROFILE_CRN;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.NAME;
import static com.sequenceiq.environment.encryptionprofile.EncryptionProfileTestConstants.USER_CRN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
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
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
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

    @InjectMocks
    private EncryptionProfileService underTest;

    @BeforeEach
    void setUp() throws TransactionService.TransactionExecutionException {
        lenient().doNothing().when(ownerAssignmentService).assignResourceOwnerRoleIfEntitled(any(), any());
        lenient().doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get())
                .when(transactionService).required(any(Supplier.class));
        lenient().when(regionAwareCrnGenerator.generateCrnStringWithUuid(any(), eq(ACCOUNT_ID))).thenReturn(ENCRYPTION_PROFILE_CRN);
    }

    @Test
    void testCreateSuccessful() throws TransactionService.TransactionExecutionException {
        when(repository.findByNameAndAccountId(NAME, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(ENCRYPTION_PROFILE);

        EncryptionProfile result = underTest.create(ENCRYPTION_PROFILE, ACCOUNT_ID, CREATOR);

        assertThat(result).isNotNull();
        assertThat(result.getResourceCrn()).isEqualTo(ENCRYPTION_PROFILE_CRN);
        assertThat(result.getName()).isEqualTo(NAME);
        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);

        verify(ownerAssignmentService).assignResourceOwnerRoleIfEntitled(CREATOR, ENCRYPTION_PROFILE_CRN);
        verify(transactionService).required(any(Supplier.class));
        verify(repository).save(ENCRYPTION_PROFILE);
    }

    @Test
    void testCreateFailsIfAlreadyExists() {
        when(repository.findByNameAndAccountId(NAME, ACCOUNT_ID))
                .thenReturn(Optional.of(ENCRYPTION_PROFILE));


        assertThatThrownBy(() -> underTest.create(ENCRYPTION_PROFILE, ACCOUNT_ID, CREATOR))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Encryption Profile already exists with name: " + NAME);

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

        assertThatThrownBy(() -> underTest.create(ENCRYPTION_PROFILE, ACCOUNT_ID, CREATOR))
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessage("HTTP 500 Internal Server Error")
                .hasCauseInstanceOf(TransactionService.TransactionExecutionException.class)
                .hasRootCauseMessage("DB error");

        verify(ownerAssignmentService).notifyResourceDeleted(ENCRYPTION_PROFILE_CRN);
    }

    @Test
    void testGetByNameAndAccountId() {
        when(repository.findByNameAndAccountId(NAME, ACCOUNT_ID)).thenReturn(Optional.of(ENCRYPTION_PROFILE));

        EncryptionProfile result = underTest.getByNameAndAccountId(NAME, ACCOUNT_ID);

        assertThat(result).isNotNull();
        assertThat(result.getResourceCrn()).isEqualTo(ENCRYPTION_PROFILE_CRN);
        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);

        verify(repository).findByNameAndAccountId(NAME, ACCOUNT_ID);
    }

    @Test
    void testGetByNameAndAccountIdNotFound() {
        when(repository.findByNameAndAccountId(NAME, ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.getByNameAndAccountId(NAME, ACCOUNT_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Encryption profile '" + NAME + "' not found.");

        verify(repository).findByNameAndAccountId(NAME, ACCOUNT_ID);
    }

    @Test
    void testGetByCrn() {
        when(repository.findByResourceCrn(ENCRYPTION_PROFILE_CRN)).thenReturn(Optional.of(ENCRYPTION_PROFILE));

        EncryptionProfile result = underTest.getByCrn(ENCRYPTION_PROFILE_CRN);

        assertThat(result).isNotNull();
        assertThat(result.getResourceCrn()).isEqualTo(ENCRYPTION_PROFILE_CRN);
        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);

        verify(repository).findByResourceCrn(ENCRYPTION_PROFILE_CRN);
    }

    @Test
    void testGetByCrnNotFound() {
        when(repository.findByResourceCrn(ENCRYPTION_PROFILE_CRN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.getByCrn(ENCRYPTION_PROFILE_CRN))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Encryption profile with crn '" + ENCRYPTION_PROFILE_CRN + "' not found.");

        verify(repository).findByResourceCrn(ENCRYPTION_PROFILE_CRN);
    }

    @Test
    void testDeleteByNameAndAccountId() {
        when(repository.findByNameAndAccountId(NAME, ACCOUNT_ID)).thenReturn(Optional.of(ENCRYPTION_PROFILE));

        EncryptionProfile result = underTest.deleteByNameAndAccountId(NAME, ACCOUNT_ID);

        assertThat(result).isNotNull();
        assertThat(result.getResourceCrn()).isEqualTo(ENCRYPTION_PROFILE_CRN);
        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);

        verify(repository).findByNameAndAccountId(NAME, ACCOUNT_ID);
        verify(repository).delete(ENCRYPTION_PROFILE);
        verify(ownerAssignmentService).notifyResourceDeleted(ENCRYPTION_PROFILE_CRN);
    }

    @Test
    void testDeleteByNameAndAccountIdNotFound() {
        when(repository.findByNameAndAccountId(NAME, ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.deleteByNameAndAccountId(NAME, ACCOUNT_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Encryption profile '" + NAME + "' not found.");

        verify(repository).findByNameAndAccountId(NAME, ACCOUNT_ID);
        verify(repository, never()).delete(any());
        verify(ownerAssignmentService, never()).notifyResourceDeleted(any());
    }

    @Test
    void testDeleteByResourceCrn() {
        when(repository.findByResourceCrn(ENCRYPTION_PROFILE_CRN)).thenReturn(Optional.of(ENCRYPTION_PROFILE));

        EncryptionProfile result = underTest.deleteByResourceCrn(ENCRYPTION_PROFILE_CRN);

        assertThat(result).isNotNull();
        assertThat(result.getResourceCrn()).isEqualTo(ENCRYPTION_PROFILE_CRN);
        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);

        verify(repository).findByResourceCrn(ENCRYPTION_PROFILE_CRN);
        verify(repository).delete(ENCRYPTION_PROFILE);
        verify(ownerAssignmentService).notifyResourceDeleted(ENCRYPTION_PROFILE_CRN);
    }

    @Test
    void testDeleteByResourceCrnNotFound() {
        when(repository.findByResourceCrn(ENCRYPTION_PROFILE_CRN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.deleteByResourceCrn(ENCRYPTION_PROFILE_CRN))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Encryption profile with crn '" + ENCRYPTION_PROFILE_CRN + "' not found.");

        verify(repository).findByResourceCrn(ENCRYPTION_PROFILE_CRN);
        verify(repository, never()).delete(any());
        verify(ownerAssignmentService, never()).notifyResourceDeleted(any());
    }

    @Test
    void testListAll() {
        when(repository.findAllByAccountId(ACCOUNT_ID)).thenReturn(Collections.singletonList(ENCRYPTION_PROFILE));

        List<EncryptionProfile> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.listAll());

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getResourceCrn()).isEqualTo(ENCRYPTION_PROFILE_CRN);
        assertThat(result.get(0).getAccountId()).isEqualTo(ACCOUNT_ID);

        verify(repository).findAllByAccountId(ACCOUNT_ID);
    }

    @Test
    void testFindAllById() {
        List<Long> ids = Collections.singletonList(1L);
        when(repository.findAllById(ids)).thenReturn(Collections.singletonList(ENCRYPTION_PROFILE));

        List<EncryptionProfile> result = underTest.findAllById(ids);

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getResourceCrn()).isEqualTo(ENCRYPTION_PROFILE_CRN);
        assertThat(result.get(0).getAccountId()).isEqualTo(ACCOUNT_ID);

        verify(repository).findAllById(ids);
    }

    @Test
    void testGetEncryptionProfilesAsAuthorizationResources() {
        when(repository.findAuthorizationResourcesByAccountId(eq(ACCOUNT_ID)))
                .thenReturn(Collections.singletonList(new ResourceWithId(ENCRYPTION_PROFILE.getId(), ENCRYPTION_PROFILE_CRN)));
        List<ResourceWithId> resources = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getEncryptionProfilesAsAuthorizationResources());

        assertThat(resources).isNotEmpty();
        assertThat(resources.get(0).getResourceCrn()).isEqualTo(ENCRYPTION_PROFILE_CRN);
        assertThat(resources.get(0).getId()).isEqualTo(ENCRYPTION_PROFILE.getId());

        verify(repository).findAuthorizationResourcesByAccountId(eq(ACCOUNT_ID));
    }

    @Test
    void testGetResourceCrnListByResourceNameList() {
        List<String> resourceNames = Collections.singletonList(NAME);
        when(repository.findAllResourceCrnByNameListAndAccountId(resourceNames, ACCOUNT_ID))
                .thenReturn(Collections.singletonList(ENCRYPTION_PROFILE_CRN));

        List<String> result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getResourceCrnListByResourceNameList(resourceNames));

        assertThat(result).isNotEmpty();
        assertThat(result.get(0)).isEqualTo(ENCRYPTION_PROFILE_CRN);

        verify(repository).findAllResourceCrnByNameListAndAccountId(resourceNames, ACCOUNT_ID);
    }

    @Test
    void testGetResourceCrnByResourceName() {
        when(repository.findResourceCrnByNameAndAccountId(NAME, ACCOUNT_ID))
                .thenReturn(Optional.of(ENCRYPTION_PROFILE_CRN));

        String result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getResourceCrnByResourceName(NAME));

        assertThat(result).isEqualTo(ENCRYPTION_PROFILE_CRN);

        verify(repository).findResourceCrnByNameAndAccountId(NAME, ACCOUNT_ID);
    }
}
