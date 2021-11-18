package com.sequenceiq.freeipa.kerberosmgmt.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.entity.KeytabCache;
import com.sequenceiq.freeipa.repository.KeytabCacheRepository;

@ExtendWith(MockitoExtension.class)
class KeytabCacheServiceTest {

    private static final String KEYTAB_PRINCIPAL = "keytabPrincipal";

    private static final String PRINCIPAL_HASH = DigestUtils.sha256Hex(KEYTAB_PRINCIPAL);

    private static final String HOSTNAME = "host";

    private static final String KEYTAB = "keytab";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:dummyUser:environment:randomGeneratedResource";

    private static final String ACCOUNT = "dummyUser";

    @Mock
    private KeytabCacheRepository keytabCacheRepository;

    @InjectMocks
    private KeytabCacheService underTest;

    @Test
    public void testSave() {
        when(keytabCacheRepository.save(any(KeytabCache.class))).thenAnswer(invocation -> invocation.getArgument(0, KeytabCache.class));

        KeytabCache result = underTest.save(ENVIRONMENT_CRN, KEYTAB_PRINCIPAL, HOSTNAME, KEYTAB);

        assertEquals(KEYTAB, result.getKeytab().getRaw());
        assertEquals(KEYTAB_PRINCIPAL, result.getPrincipal().getRaw());
        assertEquals(ENVIRONMENT_CRN, result.getEnvironmentCrn());
        assertEquals(ACCOUNT, result.getAccountId());
        assertEquals(PRINCIPAL_HASH, result.getPrincipalHash());
        assertEquals(HOSTNAME, result.getHostName());
    }

    @Test
    public void testSaveOrUpdateCachedSame() {
        KeytabCache keytabCache = new KeytabCache();
        keytabCache.setKeytab(KEYTAB);
        when(keytabCacheRepository.findByEnvironmentCrnAndPrincipalHash(ENVIRONMENT_CRN, PRINCIPAL_HASH)).thenReturn(Optional.of(keytabCache));

        KeytabCache result = underTest.saveOrUpdate(ENVIRONMENT_CRN, KEYTAB_PRINCIPAL, HOSTNAME, KEYTAB);

        assertEquals(keytabCache, result);
        verify(keytabCacheRepository, times(0)).save(any(KeytabCache.class));
    }

    @Test
    public void testSaveOrUpdateCachedDifferent() {
        KeytabCache keytabCache = new KeytabCache();
        keytabCache.setKeytab("oldone");
        when(keytabCacheRepository.findByEnvironmentCrnAndPrincipalHash(ENVIRONMENT_CRN, PRINCIPAL_HASH)).thenReturn(Optional.of(keytabCache));
        when(keytabCacheRepository.save(keytabCache)).thenAnswer(invocation -> invocation.getArgument(0, KeytabCache.class));

        KeytabCache result = underTest.saveOrUpdate(ENVIRONMENT_CRN, KEYTAB_PRINCIPAL, HOSTNAME, KEYTAB);

        assertEquals(KEYTAB, result.getKeytab().getRaw());
    }

    @Test
    public void testSaveOrUpdateNoCached() {
        when(keytabCacheRepository.findByEnvironmentCrnAndPrincipalHash(ENVIRONMENT_CRN, PRINCIPAL_HASH)).thenReturn(Optional.empty());
        when(keytabCacheRepository.save(any(KeytabCache.class))).thenAnswer(invocation -> invocation.getArgument(0, KeytabCache.class));

        KeytabCache result = underTest.saveOrUpdate(ENVIRONMENT_CRN, KEYTAB_PRINCIPAL, HOSTNAME, KEYTAB);

        assertEquals(KEYTAB, result.getKeytab().getRaw());
        assertEquals(KEYTAB_PRINCIPAL, result.getPrincipal().getRaw());
        assertEquals(ENVIRONMENT_CRN, result.getEnvironmentCrn());
        assertEquals(ACCOUNT, result.getAccountId());
        assertEquals(PRINCIPAL_HASH, result.getPrincipalHash());
        assertEquals(HOSTNAME, result.getHostName());
    }

    @Test
    public void testDeleteByEnvironmentCrnAndPrincipal() {
        when(keytabCacheRepository.deleteByEnvironmentCrnAndPrincipalHash(ENVIRONMENT_CRN, PRINCIPAL_HASH)).thenReturn(1);

        int result = underTest.deleteByEnvironmentCrnAndPrincipal(ENVIRONMENT_CRN, KEYTAB_PRINCIPAL);

        assertEquals(1, result);
    }

    @Test
    public void testDeleteByEnvironmentCrnAndHostname() {
        when(keytabCacheRepository.deleteByEnvironmentCrnAndHostName(ENVIRONMENT_CRN, HOSTNAME)).thenReturn(2);

        int result = underTest.deleteByEnvironmentCrnAndHostname(ENVIRONMENT_CRN, HOSTNAME);

        assertEquals(2, result);
    }

    @Test
    public void testDeleteByEnvironmentCrn() {
        when(keytabCacheRepository.deleteByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(3);

        int result = underTest.deleteByEnvironmentCrn(ENVIRONMENT_CRN);

        assertEquals(3, result);
    }
}