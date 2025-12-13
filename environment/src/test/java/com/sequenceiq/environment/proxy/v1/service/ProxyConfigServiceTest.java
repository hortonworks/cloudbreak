package com.sequenceiq.environment.proxy.v1.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.ws.rs.BadRequestException;

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
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.service.EnvironmentViewService;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.repository.ProxyConfigRepository;
import com.sequenceiq.environment.proxy.service.ProxyConfigModificationService;
import com.sequenceiq.environment.proxy.service.ProxyConfigService;
import com.sequenceiq.environment.proxy.v1.ProxyTestSource;

@ExtendWith(MockitoExtension.class)
public class ProxyConfigServiceTest {

    private static final String ACCOUNT_ID = "account";

    private static final String CRN = "other";

    private static final String NAME = "other";

    private static final String CREATOR = "creator";

    private static final ProxyConfig PROXY_CONFIG = ProxyTestSource.getProxyConfig();

    @Mock
    private ProxyConfigRepository proxyConfigRepository;

    @Mock
    private EnvironmentViewService environmentViewService;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Mock
    private TransactionService transactionService;

    @Mock
    private OwnerAssignmentService ownerAssignmentService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ProxyConfigModificationService proxyConfigModificationService;

    @InjectMocks
    private ProxyConfigService underTestProxyConfigService;

    @BeforeEach
    public void setUp() throws TransactionService.TransactionExecutionException {
        lenient().doNothing().when(ownerAssignmentService).assignResourceOwnerRoleIfEntitled(any(), any());
        lenient().doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any(Supplier.class));
    }

    @Test
    public void testGetValidResult() {
        final long id = 1L;
        when(proxyConfigRepository.findById(id)).thenReturn(Optional.of(PROXY_CONFIG));
        assertEquals(PROXY_CONFIG, underTestProxyConfigService.get(id));
    }

    @Test
    public void testGetNullAndNotFound() {
        final long id = 2L;
        when(proxyConfigRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> underTestProxyConfigService.get(id));
    }

    @Test
    public void testGetByNameForAccountIdEmpty() {
        when(proxyConfigRepository.findByNameInAccount(NAME, ACCOUNT_ID)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> underTestProxyConfigService.getByNameForAccountId(NAME, ACCOUNT_ID));
    }

    @Test
    public void testGetByNameForAccountIdHasResult() {
        when(proxyConfigRepository.findByNameInAccount(NAME, ACCOUNT_ID)).thenReturn(Optional.of(PROXY_CONFIG));
        assertEquals(PROXY_CONFIG, underTestProxyConfigService.getByNameForAccountId(NAME, ACCOUNT_ID));
    }

    @Test
    public void testGetByCrnForAccountIdEmpty() {
        when(proxyConfigRepository.findByResourceCrnInAccount(CRN, ACCOUNT_ID)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> underTestProxyConfigService.getByCrnForAccountId(CRN, ACCOUNT_ID));
    }

    @Test
    public void testGetByCrnForAccountIdHasResult() {
        when(proxyConfigRepository.findByResourceCrnInAccount(CRN, ACCOUNT_ID)).thenReturn(Optional.of(PROXY_CONFIG));
        assertEquals(PROXY_CONFIG, underTestProxyConfigService.getByCrnForAccountId(CRN, ACCOUNT_ID));
    }

    @Test
    public void testListInAccountEmpty() {
        when(proxyConfigRepository.findAllInAccount(ACCOUNT_ID)).thenReturn(Set.of());
        assertTrue(underTestProxyConfigService.listInAccount(ACCOUNT_ID).isEmpty(), "lisInAccount should be empty but it is not");
    }

    @Test
    public void testListInAccountWithResult() {
        Set<ProxyConfig> proxyConfigs = Set.of(PROXY_CONFIG);
        when(proxyConfigRepository.findAllInAccount(ACCOUNT_ID)).thenReturn(proxyConfigs);
        assertEquals(proxyConfigs, underTestProxyConfigService.listInAccount(ACCOUNT_ID));
    }

    @Test
    public void testCreate() {
        when(proxyConfigRepository.save(PROXY_CONFIG)).thenReturn(PROXY_CONFIG);
        assertDoesNotThrow(() -> underTestProxyConfigService.create(PROXY_CONFIG, ACCOUNT_ID, CREATOR));
    }

    @Test
    public void testCreateAlreadyExist() {
        when(proxyConfigRepository.findResourceCrnByNameAndTenantId(anyString(), any())).thenReturn(Optional.of(PROXY_CONFIG.getName()));
        assertThrows(BadRequestException.class, () -> underTestProxyConfigService.create(PROXY_CONFIG, ACCOUNT_ID, CREATOR));
    }

    @Test
    public void testCreateOtherException() {
        Class<JDBCException> throwableType = JDBCException.class;
        when(proxyConfigRepository.save(PROXY_CONFIG)).thenThrow(throwableType);
        assertThrows(throwableType, () -> underTestProxyConfigService.create(PROXY_CONFIG, ACCOUNT_ID, CREATOR));
    }

    @Test
    public void testDeleteByCrnInAccountNonExisting() {
        when(proxyConfigRepository.findByResourceCrnInAccount(CRN, ACCOUNT_ID)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> underTestProxyConfigService.deleteByCrnInAccount(CRN, ACCOUNT_ID));
    }

    @Test
    public void testDeleteByCrnInAccountExisting() {
        when(proxyConfigRepository.findByResourceCrnInAccount(CRN, ACCOUNT_ID)).thenReturn(Optional.of(PROXY_CONFIG));
        assertEquals(PROXY_CONFIG, underTestProxyConfigService.deleteByCrnInAccount(CRN, ACCOUNT_ID));
    }

    @Test
    public void testDeleteByCrnOtherException() {
        Class<JDBCException> throwableType = JDBCException.class;
        when(proxyConfigRepository.findByResourceCrnInAccount(CRN, ACCOUNT_ID)).thenThrow(throwableType);
        assertThrows(throwableType, () -> underTestProxyConfigService.deleteByCrnInAccount(CRN, ACCOUNT_ID));
    }

    @Test
    public void testDeleteByNameForAccountIdEmpty() {
        when(proxyConfigRepository.findByNameInAccount(NAME, ACCOUNT_ID)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> underTestProxyConfigService.deleteByNameInAccount(NAME, ACCOUNT_ID));
    }

    @Test
    public void testDeleteByNameForAccountIdHasResultAndNotAssignedToAnyEnvThenDeleteSuccess() {
        when(environmentViewService.findAllByProxyConfigIdAndArchivedIsFalse(any())).thenReturn(Set.of());
        when(proxyConfigRepository.findByNameInAccount(NAME, ACCOUNT_ID)).thenReturn(Optional.of(PROXY_CONFIG));
        assertEquals(PROXY_CONFIG, underTestProxyConfigService.deleteByNameInAccount(NAME, ACCOUNT_ID));
    }

    @Test
    public void testDeleteByNameForAccountIdHasResultAssignedToAnyEnvThenDeleteFails() {
        EnvironmentView environmentView = new EnvironmentView();
        environmentView.setName("env1");
        when(environmentViewService.findAllByProxyConfigIdAndArchivedIsFalse(any())).thenReturn(Set.of(environmentView));
        when(proxyConfigRepository.findByNameInAccount(NAME, ACCOUNT_ID)).thenReturn(Optional.of(PROXY_CONFIG));
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTestProxyConfigService.deleteByNameInAccount(NAME, ACCOUNT_ID));
        assertEquals(badRequestException.getMessage(),
                "Proxy Configuration 'name' cannot be deleted because the following environments are using it: [env1].");
    }

    @Test
    public void testDeleteByCrnForAccountIdHasResultAssignedToAnyEnvThenDeleteFails() {
        EnvironmentView environmentView = new EnvironmentView();
        environmentView.setName("env1");
        when(environmentViewService.findAllByProxyConfigIdAndArchivedIsFalse(any())).thenReturn(Set.of(environmentView));
        when(proxyConfigRepository.findByResourceCrnInAccount(CRN, ACCOUNT_ID)).thenReturn(Optional.of(PROXY_CONFIG));
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTestProxyConfigService.deleteByCrnInAccount(CRN, ACCOUNT_ID));
        assertEquals(badRequestException.getMessage(),
                "Proxy Configuration 'name' cannot be deleted because the following environments are using it: [env1].");
    }

    @Test
    public void testDeleteByCrnForAccountIdHasResultAndNotAssignedToAnyEnvThenDeleteSuccess() {
        when(environmentViewService.findAllByProxyConfigIdAndArchivedIsFalse(any())).thenReturn(Set.of());
        when(proxyConfigRepository.findByResourceCrnInAccount(CRN, ACCOUNT_ID)).thenReturn(Optional.of(PROXY_CONFIG));
        assertEquals(PROXY_CONFIG, underTestProxyConfigService.deleteByCrnInAccount(CRN, ACCOUNT_ID));
    }

    @Test
    public void testDeleteByNameForAccountOtherException() {
        Class<JDBCException> throwableType = JDBCException.class;
        when(proxyConfigRepository.findByNameInAccount(NAME, ACCOUNT_ID)).thenThrow(throwableType);
        assertThrows(throwableType, () -> underTestProxyConfigService.deleteByNameInAccount(NAME, ACCOUNT_ID));
    }

    @Test
    public void testDeleteMultipleInAccountEmpty() {
        Set<String> names = Set.of(NAME);
        when(proxyConfigRepository.findByNameOrResourceCrnInAccount(names, ACCOUNT_ID)).thenReturn(Set.of());
        assertThrows(NotFoundException.class, () -> underTestProxyConfigService.deleteMultipleInAccount(names, ACCOUNT_ID));
    }

    @Test
    public void testDeleteMultipleInAccountHasResultButNotAll() {
        String name1 = ProxyTestSource.NAME;
        String name2 = "another";
        Set<String> names = Set.of(name1, name2);
        when(proxyConfigRepository.findByNameOrResourceCrnInAccount(names, ACCOUNT_ID)).thenReturn(Set.of(PROXY_CONFIG));
        assertThrows(NotFoundException.class, () -> underTestProxyConfigService.deleteMultipleInAccount(names, ACCOUNT_ID));
    }

    @Test
    public void testDeleteMultipleInAccountHasAllResult() {
        String name1 = ProxyTestSource.NAME;
        String name2 = "another";
        Set<String> names = Set.of(name1, name2);
        ProxyConfig proxyConfig1 = ProxyTestSource.getProxyConfig();
        ProxyConfig proxyConfig2 = ProxyTestSource.getProxyConfig();
        proxyConfig2.setName(name2);
        proxyConfig2.setId(2L);
        Set<ProxyConfig> proxyConfigs = Set.of(proxyConfig2, proxyConfig1);
        when(proxyConfigRepository.findByNameOrResourceCrnInAccount(names, ACCOUNT_ID)).thenReturn(proxyConfigs);
        assertEquals(proxyConfigs, underTestProxyConfigService.deleteMultipleInAccount(names, ACCOUNT_ID));
    }
}
