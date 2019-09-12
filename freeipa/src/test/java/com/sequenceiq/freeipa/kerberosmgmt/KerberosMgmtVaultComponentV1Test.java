package com.sequenceiq.freeipa.kerberosmgmt;

import static org.mockito.ArgumentMatchers.anyString;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.Mock;
import org.mockito.Mockito;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.HostKeytabRequest;
import com.sequenceiq.freeipa.api.v1.kerberosmgmt.model.ServiceKeytabRequest;
import com.sequenceiq.freeipa.kerberosmgmt.v1.KerberosMgmtVaultComponent;

@ExtendWith(MockitoExtension.class)
public class KerberosMgmtVaultComponentV1Test {
    private static final String ENVIRONMENT_ID = "crn:cdp:environments:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:12345-6789";

    private static final String CLUSTER_ID = "crn:cdp:datalake:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:datalake:54321-9876";

    private static final String HOST = "host1";

    private static final String ACCOUNT = "account1";

    private static final String KEYTAB = "keytab1";

    private static final String PRINCIPAL = "principal1";

    private static final String SERVICE = "service1";

    private static final String SECRET = "secret1";

    private static final String ENGINE_PATH = "engine_path";

    @Mock
    private SecretService secretService;

    @Mock
    private StringToSecretResponseConverter stringToSecretResponseConverter;

    @InjectMocks
    private KerberosMgmtVaultComponent underTest;

    @Test
    public void testCleanupSecretsForCluster() throws Exception {
        List<String> emptyListing = new ArrayList<>();
        Mockito.when(secretService.listEntries(anyString())).thenReturn(emptyListing);
        underTest.cleanupSecrets(ENVIRONMENT_ID, CLUSTER_ID, ACCOUNT);
        Mockito.verify(secretService).cleanup("account1/HostKeytab/serviceprincipal/12345-6789/54321-9876/");
        Mockito.verify(secretService).cleanup("account1/HostKeytab/keytab/12345-6789/54321-9876/");
        Mockito.verify(secretService).cleanup("account1/ServiceKeytab/serviceprincipal/12345-6789/54321-9876/");
        Mockito.verify(secretService).cleanup("account1/ServiceKeytab/keytab/12345-6789/54321-9876/");
    }

    @Test
    public void testCleanupSecretsForWholeEnvironment() throws Exception {
        List<String> emptyListing = new ArrayList<>();
        Mockito.when(secretService.listEntries(anyString())).thenReturn(emptyListing);
        underTest.cleanupSecrets(ENVIRONMENT_ID, null, ACCOUNT);
        Mockito.verify(secretService).cleanup("account1/HostKeytab/serviceprincipal/12345-6789/");
        Mockito.verify(secretService).cleanup("account1/HostKeytab/keytab/12345-6789/");
        Mockito.verify(secretService).cleanup("account1/ServiceKeytab/serviceprincipal/12345-6789/");
        Mockito.verify(secretService).cleanup("account1/ServiceKeytab/keytab/12345-6789/");
    }

    @Test
    public void testRecursivelyCleanupVault() throws Exception {
        String dir1 = "basedir/foo/";
        String subdir1 = "bar1/";
        String subdir2 = "bar2/";
        String file1 = "baz1";
        String file2 = "baz2";
        List<String> dir1Listing = new ArrayList<>();
        dir1Listing.add(subdir1);
        dir1Listing.add(subdir2);
        List<String> subdir1Listing = new ArrayList<>();
        subdir1Listing.add(file1);
        List<String> subdir2Listing = new ArrayList<>();
        subdir2Listing.add(file1);
        subdir2Listing.add(file2);
        List<String> fileListing = new ArrayList<>();
        Mockito.when(secretService.listEntries(dir1)).thenReturn(dir1Listing);
        Mockito.when(secretService.listEntries(dir1 + subdir1)).thenReturn(subdir1Listing);
        Mockito.when(secretService.listEntries(dir1 + subdir2)).thenReturn(subdir2Listing);
        Mockito.when(secretService.listEntries(dir1 + subdir1 + file1)).thenReturn(fileListing);
        Mockito.when(secretService.listEntries(dir1 + subdir2 + file1)).thenReturn(fileListing);
        Mockito.when(secretService.listEntries(dir1 + subdir2 + file2)).thenReturn(fileListing);
        underTest.recursivelyCleanupVault(dir1);
        Mockito.verify(secretService).cleanup(dir1 + subdir1 + file1);
        Mockito.verify(secretService).cleanup(dir1 + subdir2 + file1);
        Mockito.verify(secretService).cleanup(dir1 + subdir2 + file2);
        Mockito.verifyNoMoreInteractions(secretService);
    }

    @Test
    public void testGetSecretResponseForPrincipalWithService() throws Exception {
        String expectedPath = "account1/ServiceKeytab/serviceprincipal/12345-6789/54321-9876/host1/service1";
        SecretResponse expectedSecretResponse = new SecretResponse();
        expectedSecretResponse.setEnginePath(ENGINE_PATH);
        expectedSecretResponse.setSecretPath(expectedPath);
        ServiceKeytabRequest serviceKeytabRequest = new ServiceKeytabRequest();
        serviceKeytabRequest.setEnvironmentCrn(ENVIRONMENT_ID);
        serviceKeytabRequest.setClusterCrn(CLUSTER_ID);
        serviceKeytabRequest.setServerHostName(HOST);
        serviceKeytabRequest.setServiceName(SERVICE);
        Mockito.when(secretService.put(anyString(), anyString())).thenReturn(SECRET);
        Mockito.when(stringToSecretResponseConverter.convert(anyString())).thenReturn(expectedSecretResponse);
        Assertions.assertEquals(expectedSecretResponse, underTest.getSecretResponseForPrincipal(serviceKeytabRequest, ACCOUNT, PRINCIPAL));
        Mockito.verify(secretService).put(expectedPath, PRINCIPAL);
        Mockito.verify(stringToSecretResponseConverter).convert(SECRET);
    }

    @Test
    public void testGetSecretResponseForKeytabWithService() throws Exception {
        String expectedPath = "account1/ServiceKeytab/keytab/12345-6789/54321-9876/host1/service1";
        SecretResponse expectedSecretResponse = new SecretResponse();
        expectedSecretResponse.setEnginePath(ENGINE_PATH);
        expectedSecretResponse.setSecretPath(expectedPath);
        ServiceKeytabRequest serviceKeytabRequest = new ServiceKeytabRequest();
        serviceKeytabRequest.setEnvironmentCrn(ENVIRONMENT_ID);
        serviceKeytabRequest.setClusterCrn(CLUSTER_ID);
        serviceKeytabRequest.setServerHostName(HOST);
        serviceKeytabRequest.setServiceName(SERVICE);
        Mockito.when(secretService.put(anyString(), anyString())).thenReturn(SECRET);
        Mockito.when(stringToSecretResponseConverter.convert(anyString())).thenReturn(expectedSecretResponse);
        Assertions.assertEquals(expectedSecretResponse, underTest.getSecretResponseForKeytab(serviceKeytabRequest, ACCOUNT, KEYTAB));
        Mockito.verify(secretService).put(expectedPath, KEYTAB);
        Mockito.verify(stringToSecretResponseConverter).convert(SECRET);
    }

    @Test
    public void testGetSecretResponseForPrincipalWithHost() throws Exception {
        String expectedPath = "account1/HostKeytab/serviceprincipal/12345-6789/54321-9876/host1";
        SecretResponse expectedSecretResponse = new SecretResponse();
        expectedSecretResponse.setEnginePath(ENGINE_PATH);
        expectedSecretResponse.setSecretPath(expectedPath);
        HostKeytabRequest hostKeytabRequest = new HostKeytabRequest();
        hostKeytabRequest.setEnvironmentCrn(ENVIRONMENT_ID);
        hostKeytabRequest.setClusterCrn(CLUSTER_ID);
        hostKeytabRequest.setServerHostName(HOST);
        Mockito.when(secretService.put(anyString(), anyString())).thenReturn(SECRET);
        Mockito.when(stringToSecretResponseConverter.convert(anyString())).thenReturn(expectedSecretResponse);
        Assertions.assertEquals(expectedSecretResponse, underTest.getSecretResponseForPrincipal(hostKeytabRequest, ACCOUNT, PRINCIPAL));
        Mockito.verify(secretService).put(expectedPath, PRINCIPAL);
        Mockito.verify(stringToSecretResponseConverter).convert(SECRET);
    }

    @Test
    public void testGetSecretResponseForKeytabWithHost() throws Exception {
        String expectedPath = "account1/HostKeytab/keytab/12345-6789/54321-9876/host1";
        SecretResponse expectedSecretResponse = new SecretResponse();
        expectedSecretResponse.setEnginePath(ENGINE_PATH);
        expectedSecretResponse.setSecretPath(expectedPath);
        HostKeytabRequest hostKeytabRequest = new HostKeytabRequest();
        hostKeytabRequest.setEnvironmentCrn(ENVIRONMENT_ID);
        hostKeytabRequest.setClusterCrn(CLUSTER_ID);
        hostKeytabRequest.setServerHostName(HOST);
        Mockito.when(secretService.put(anyString(), anyString())).thenReturn(SECRET);
        Mockito.when(stringToSecretResponseConverter.convert(anyString())).thenReturn(expectedSecretResponse);
        Assertions.assertEquals(expectedSecretResponse, underTest.getSecretResponseForKeytab(hostKeytabRequest, ACCOUNT, KEYTAB));
        Mockito.verify(secretService).put(expectedPath, KEYTAB);
        Mockito.verify(stringToSecretResponseConverter).convert(SECRET);
    }

}