package com.sequenceiq.environment.proxy.v1.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.hibernate.JDBCException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.repository.ProxyConfigRepository;
import com.sequenceiq.environment.proxy.service.ProxyConfigService;
import com.sequenceiq.environment.proxy.v1.ProxyTestSource;

public class ProxyConfigServiceTest {

    private static final String ACCOUNT_ID = "account";

    private static final String CRN = "other";

    private static final String NAME = "other";

    private static final String CREATOR = "creator";

    private static final ProxyConfig PROXY_CONFIG = ProxyTestSource.getProxyConfig();

    @Mock
    private final ProxyConfigRepository proxyConfigRepository;

    @Mock
    private final RegionAwareCrnGenerator regionAwareCrnGenerator;

    private ProxyConfigService underTestProxyConfigService;

    public ProxyConfigServiceTest() {
        proxyConfigRepository = mock(ProxyConfigRepository.class);
        regionAwareCrnGenerator = mock(RegionAwareCrnGenerator.class);
        underTestProxyConfigService = new ProxyConfigService(proxyConfigRepository, regionAwareCrnGenerator);
    }

    @Test
    public void testGetValidResult() {
        final long id = 1L;
        when(proxyConfigRepository.findById(id)).thenReturn(Optional.of(PROXY_CONFIG));
        Assertions.assertEquals(PROXY_CONFIG, underTestProxyConfigService.get(id));
    }

    @Test
    public void testGetNullAndNotFound() {
        final long id = 2L;
        when(proxyConfigRepository.findById(id)).thenReturn(Optional.empty());
        Assertions.assertThrows(NotFoundException.class, () -> underTestProxyConfigService.get(id));
    }

    @Test
    public void testGetByNameForAccountIdEmpty() {
        when(proxyConfigRepository.findByNameInAccount(NAME, ACCOUNT_ID)).thenReturn(Optional.empty());
        Assertions.assertThrows(NotFoundException.class, () -> underTestProxyConfigService.getByNameForAccountId(NAME, ACCOUNT_ID));
    }

    @Test
    public void testGetByNameForAccountIdHasResult() {
        when(proxyConfigRepository.findByNameInAccount(NAME, ACCOUNT_ID)).thenReturn(Optional.of(PROXY_CONFIG));
        Assertions.assertEquals(PROXY_CONFIG, underTestProxyConfigService.getByNameForAccountId(NAME, ACCOUNT_ID));
    }

    @Test
    public void testGetByCrnForAccountIdEmpty() {
        when(proxyConfigRepository.findByResourceCrnInAccount(CRN, ACCOUNT_ID)).thenReturn(Optional.empty());
        Assertions.assertThrows(NotFoundException.class, () -> underTestProxyConfigService.getByCrnForAccountId(CRN, ACCOUNT_ID));
    }

    @Test
    public void testGetByCrnForAccountIdHasResult() {
        when(proxyConfigRepository.findByResourceCrnInAccount(CRN, ACCOUNT_ID)).thenReturn(Optional.of(PROXY_CONFIG));
        Assertions.assertEquals(PROXY_CONFIG, underTestProxyConfigService.getByCrnForAccountId(CRN, ACCOUNT_ID));
    }

    @Test
    public void testListInAccountEmpty() {
        when(proxyConfigRepository.findAllInAccount(ACCOUNT_ID)).thenReturn(Set.of());
        Assertions.assertTrue(underTestProxyConfigService.listInAccount(ACCOUNT_ID).isEmpty(), "lisInAccount should be empty but it is not");
    }

    @Test
    public void testListInAccountWithResult() {
        Set<ProxyConfig> proxyConfigs = Set.of(PROXY_CONFIG);
        when(proxyConfigRepository.findAllInAccount(ACCOUNT_ID)).thenReturn(proxyConfigs);
        Assertions.assertEquals(proxyConfigs, underTestProxyConfigService.listInAccount(ACCOUNT_ID));
    }

    @Test
    public void testCreate() {
        when(proxyConfigRepository.save(PROXY_CONFIG)).thenReturn(PROXY_CONFIG);
        Assertions.assertDoesNotThrow(() -> underTestProxyConfigService.create(PROXY_CONFIG, ACCOUNT_ID, CREATOR));
    }

    @Test
    public void testCreateAlreadyExist() {
        when(proxyConfigRepository.save(PROXY_CONFIG)).thenThrow(DataIntegrityViolationException.class);
        Assertions.assertThrows(AccessDeniedException.class, () -> underTestProxyConfigService.create(PROXY_CONFIG, ACCOUNT_ID, CREATOR));
    }

    @Test
    public void testCreateOtherException() {
        Class<JDBCException> throwableType = JDBCException.class;
        when(proxyConfigRepository.save(PROXY_CONFIG)).thenThrow(throwableType);
        Assertions.assertThrows(throwableType, () -> underTestProxyConfigService.create(PROXY_CONFIG, ACCOUNT_ID, CREATOR));
    }

    @Test
    public void testDeleteByCrnInAccountNonExisting() {
        when(proxyConfigRepository.findByResourceCrnInAccount(CRN, ACCOUNT_ID)).thenReturn(Optional.empty());
        Assertions.assertThrows(NotFoundException.class, () -> underTestProxyConfigService.deleteByCrnInAccount(CRN, ACCOUNT_ID));
    }

    @Test
    public void testDeleteByCrnInAccountExisting() {
        when(proxyConfigRepository.findByResourceCrnInAccount(CRN, ACCOUNT_ID)).thenReturn(Optional.of(PROXY_CONFIG));
        Assertions.assertEquals(PROXY_CONFIG, underTestProxyConfigService.deleteByCrnInAccount(CRN, ACCOUNT_ID));
    }

    @Test
    public void testDeleteByCrnOtherException() {
        Class<JDBCException> throwableType = JDBCException.class;
        when(proxyConfigRepository.findByResourceCrnInAccount(CRN, ACCOUNT_ID)).thenThrow(throwableType);
        Assertions.assertThrows(throwableType, () -> underTestProxyConfigService.deleteByCrnInAccount(CRN, ACCOUNT_ID));
    }

    @Test
    public void testDeleteByNameForAccountIdEmpty() {
        when(proxyConfigRepository.findByNameInAccount(NAME, ACCOUNT_ID)).thenReturn(Optional.empty());
        Assertions.assertThrows(NotFoundException.class, () -> underTestProxyConfigService.deleteByNameInAccount(NAME, ACCOUNT_ID));
    }

    @Test
    public void testDeleteByNameForAccountIdHasResult() {
        when(proxyConfigRepository.findByNameInAccount(NAME, ACCOUNT_ID)).thenReturn(Optional.of(PROXY_CONFIG));
        Assertions.assertEquals(PROXY_CONFIG, underTestProxyConfigService.deleteByNameInAccount(NAME, ACCOUNT_ID));
    }

    @Test
    public void testDeleteByNameForAccountOtherException() {
        Class<JDBCException> throwableType = JDBCException.class;
        when(proxyConfigRepository.findByNameInAccount(NAME, ACCOUNT_ID)).thenThrow(throwableType);
        Assertions.assertThrows(throwableType, () -> underTestProxyConfigService.deleteByNameInAccount(NAME, ACCOUNT_ID));
    }

    @Test
    public void testDeleteMultipleInAccountEmpty() {
        Set<String> names = Set.of(NAME);
        when(proxyConfigRepository.findByNameOrResourceCrnInAccount(names, ACCOUNT_ID)).thenReturn(Set.of());
        Assertions.assertThrows(NotFoundException.class, () -> underTestProxyConfigService.deleteMultipleInAccount(names, ACCOUNT_ID));
    }

    @Test
    public void testDeleteMultipleInAccountHasResultButNotAll() {
        String name1 = ProxyTestSource.NAME;
        String name2 = "another";
        Set<String> names = Set.of(name1, name2);
        when(proxyConfigRepository.findByNameOrResourceCrnInAccount(names, ACCOUNT_ID)).thenReturn(Set.of(PROXY_CONFIG));
        Assertions.assertThrows(NotFoundException.class, () -> underTestProxyConfigService.deleteMultipleInAccount(names, ACCOUNT_ID));
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
        Assertions.assertEquals(proxyConfigs, underTestProxyConfigService.deleteMultipleInAccount(names, ACCOUNT_ID));
    }
}
