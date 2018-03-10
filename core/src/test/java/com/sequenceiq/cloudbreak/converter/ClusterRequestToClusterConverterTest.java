package com.sequenceiq.cloudbreak.converter;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

public class ClusterRequestToClusterConverterTest extends AbstractJsonConverterTest<ClusterRequest> {

    @InjectMocks
    private ClusterRequestToClusterConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Before
    public void setUp() {
        underTest = new ClusterRequestToClusterConverter();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        Cluster result = underTest.convert(getRequest("stack/cluster.json"));
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted", "creationFinished", "upSince", "statusReason", "ambariIp",
                "ambariStackDetails", "fileSystem", "certDir", "rdsConfigs", "ldapConfig", "attributes", "blueprintCustomProperties", "uptime",
                "kerberosConfig", "ambariSecurityMasterKey", "proxyConfig", "extendedBlueprintText"));
    }

    @Test
    public void testConvertWithFileSystemDetails() {
        // GIVEN
        given(conversionService.convert(any(KerberosRequest.class), eq(KerberosConfig.class))).willReturn(new KerberosConfig());
        given(conversionService.convert(any(FileSystemRequest.class), eq(FileSystem.class))).willReturn(new FileSystem());
        // WHEN
        Cluster result = underTest.convert(getRequest("stack/cluster-with-file-system.json"));
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted", "creationFinished", "upSince", "statusReason", "ambariIp",
                "ambariStackDetails", "certDir", "rdsConfigs", "ldapConfig", "attributes", "blueprintCustomProperties", "uptime",
                "ambariSecurityMasterKey", "proxyConfig", "extendedBlueprintText"));
    }

    @Test
    public void testNoGateway() {
        // GIVEN
        // WHEN
        Cluster result = underTest.convert(getRequest("stack/cluster-no-gateway.json"));
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted", "creationFinished", "upSince", "statusReason", "ambariIp",
                "ambariStackDetails", "fileSystem", "certDir", "rdsConfigs", "ldapConfig", "attributes", "blueprintCustomProperties", "uptime",
                "kerberosConfig", "ambariSecurityMasterKey", "proxyConfig", "extendedBlueprintText"));

        assertAllFieldsNotNull(result.getGateway(), Arrays.asList("id", "ssoProvider", "signKey", "signPub", "signCert", "tokenCert"));
    }

    @Override
    public Class<ClusterRequest> getRequestClass() {
        return ClusterRequest.class;
    }
}
