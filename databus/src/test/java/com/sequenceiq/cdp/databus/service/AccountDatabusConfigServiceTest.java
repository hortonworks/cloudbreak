package com.sequenceiq.cdp.databus.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cdp.databus.altus.DatabusMachineUserProvider;
import com.sequenceiq.cdp.databus.cache.AccountDatabusConfigCache;
import com.sequenceiq.cdp.databus.entity.AccountDatabusConfig;
import com.sequenceiq.cdp.databus.model.exception.DatabusRecordProcessingException;
import com.sequenceiq.cdp.databus.repository.AccountDatabusConfigRepository;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;

@ExtendWith(MockitoExtension.class)
public class AccountDatabusConfigServiceTest {

    @InjectMocks
    private AccountDatabusConfigService underTest;

    @Mock
    private AccountDatabusConfigRepository accountDatabusConfigRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private DatabusMachineUserProvider databusMachineUserProvider;

    @Mock
    private AccountDatabusConfigCache accountDatabusConfigCache;

    @Mock
    private AccountDatabusConfig accountDatabusConfig;

    @Mock
    private DataBusCredential dataBusCredential;

    @BeforeEach
    public void setUp() {
        underTest = new AccountDatabusConfigService(accountDatabusConfigRepository, transactionService, databusMachineUserProvider);
    }

    @Test
    public void testGetOrCreateDataBusCredentials() throws DatabusRecordProcessingException {
        // GIVEN
        given(accountDatabusConfigCache.containsCredentialKey(anyString())).willReturn(true);
        given(accountDatabusConfigCache.getCredential(anyString())).willReturn(dataBusCredential);
        // WHEN
        DataBusCredential result = underTest.getOrCreateDataBusCredentials("machineUser", "accountId", accountDatabusConfigCache);
        // THEN
        assertEquals(dataBusCredential, result);
        verify(accountDatabusConfigCache, times(1)).containsCredentialKey(anyString());
        verify(accountDatabusConfigCache, times(1)).getCredential(anyString());
    }

    @Test
    public void testGetOrCreateDataBusCredentialsWithEmptyCacheAndPersisted()
            throws DatabusRecordProcessingException, TransactionService.TransactionExecutionException {
        // GIVEN
        String credentialJson = "{\"machineUser\": \"name\", \"accessKey\": \"accessKey\", \"privateKey\": \"privateKey\"}";
        AccountDatabusConfigService underTestSpy = spy(underTest);
        given(accountDatabusConfigCache.containsCredentialKey(anyString())).willReturn(false);
        doReturn(Optional.of(accountDatabusConfig)).when(underTestSpy).findByNameAndAccountId(anyString(), anyString());
        given(accountDatabusConfig.getDatabusCredential()).willReturn(credentialJson);
        // WHEN
        DataBusCredential result = underTestSpy.getOrCreateDataBusCredentials("machineUser", "cloudera", accountDatabusConfigCache);
        // THEN
        assertEquals("accessKey", result.getAccessKey());
    }

    @Test
    public void testGetOrCreateDataBusCredentialsWithEmptyCacheAndNotPersisted()
            throws DatabusRecordProcessingException, TransactionService.TransactionExecutionException {
        // GIVEN
        AccountDatabusConfigService underTestSpy = spy(underTest);
        given(accountDatabusConfigCache.containsCredentialKey(anyString())).willReturn(false);
        doReturn(Optional.empty()).when(underTestSpy).findByNameAndAccountId(anyString(), anyString());
        given(databusMachineUserProvider.getOrCreateMachineUserAndAccessKey(anyString(), anyString())).willReturn(createAltusCredential());
        doNothing().when(underTestSpy).save(any(AccountDatabusConfig.class));
        // WHEN
        underTestSpy.getOrCreateDataBusCredentials("machineUser", "cloudera", accountDatabusConfigCache);
        // THEN
        verify(underTestSpy, times(1)).save(any(AccountDatabusConfig.class));
    }

    @Test
    public void testGetOrCreateDataBusCredentialsWithEmptyCacheAndPersistFailue()
            throws TransactionService.TransactionExecutionException {
        // GIVEN
        AccountDatabusConfigService underTestSpy = spy(underTest);
        given(accountDatabusConfigCache.containsCredentialKey(anyString())).willReturn(false);
        doReturn(Optional.empty()).when(underTestSpy).findByNameAndAccountId(anyString(), anyString());
        given(databusMachineUserProvider.getOrCreateMachineUserAndAccessKey(anyString(), anyString())).willReturn(createAltusCredential());
        doThrow(new TransactionService.TransactionExecutionException("transaction error", new RuntimeException()))
                .when(underTestSpy).save(any(AccountDatabusConfig.class));
        // WHEN
        DatabusRecordProcessingException result = assertThrows(DatabusRecordProcessingException.class, () ->
                underTestSpy.getOrCreateDataBusCredentials("machineUser", "cloudera", accountDatabusConfigCache));
        // THEN
        assertTrue(result.getMessage().contains("Error during persisting account databus config in db."));
    }

    @Test
    public void testCleanupCacheAndDbForAccountIdAndName() throws TransactionService.TransactionExecutionException {
        // GIVEN
        doNothing().when(transactionService).required(any(Runnable.class));
        given(accountDatabusConfigCache.containsCredentialKey(anyString())).willReturn(true);
        doNothing().when(accountDatabusConfigCache).removeCredential(anyString());
        // WHEN
        underTest.cleanupCacheAndDbForAccountIdAndName("machineUser", "cloudera", accountDatabusConfigCache);
        // THEN
        verify(transactionService, times(1)).required(any(Runnable.class));
        verify(accountDatabusConfigCache, times(1)).containsCredentialKey(anyString());
        verify(accountDatabusConfigCache, times(1)).removeCredential(anyString());
    }

    @Test
    public void testCheckMachineUserWithAccessKeyStillExists() {
        // GIVEN
        given(accountDatabusConfigCache.containsCredentialKey(anyString())).willReturn(true);
        given(accountDatabusConfigCache.getCredential(anyString())).willReturn(dataBusCredential);
        given(databusMachineUserProvider.isDataBusCredentialStillExist("cloudera", dataBusCredential)).willReturn(true);
        // WHEN
        boolean result = underTest.checkMachineUserWithAccessKeyStillExists("cloudera", accountDatabusConfigCache);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testCheckMachineUserWithAccessKeyDoesNotExistInUMS() {
        // GIVEN
        given(accountDatabusConfigCache.containsCredentialKey(anyString())).willReturn(true);
        given(accountDatabusConfigCache.getCredential(anyString())).willReturn(dataBusCredential);
        given(databusMachineUserProvider.isDataBusCredentialStillExist("cloudera", dataBusCredential)).willReturn(false);
        // WHEN
        boolean result = underTest.checkMachineUserWithAccessKeyStillExists("cloudera", accountDatabusConfigCache);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testCheckMachineUserWithAccessKeyDoesNotExistInCache() {
        // GIVEN
        given(accountDatabusConfigCache.containsCredentialKey(anyString())).willReturn(false);
        // WHEN
        boolean result = underTest.checkMachineUserWithAccessKeyStillExists("cloudera", accountDatabusConfigCache);
        // THEN
        assertFalse(result);
    }

    private Optional<AltusCredential> createAltusCredential() {
        final AltusCredential credential = new AltusCredential("accessKey", "privateKey".toCharArray());
        return Optional.of(credential);
    }

}
