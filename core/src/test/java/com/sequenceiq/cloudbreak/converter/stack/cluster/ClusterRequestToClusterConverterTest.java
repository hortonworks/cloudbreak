package com.sequenceiq.cloudbreak.converter.stack.cluster;

import static org.junit.Assert.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractJsonConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ClusterV4RequestToClusterConverter;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemConfigService;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosService;

public class ClusterRequestToClusterConverterTest extends AbstractJsonConverterTest<ClusterV4Request> {

    @InjectMocks
    private ClusterV4RequestToClusterConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Mock
    private FileSystemConfigService fileSystemConfigService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private KerberosService kerberosService;

    @Before
    public void setUp() {
        underTest = new ClusterV4RequestToClusterConverter();
        Whitebox.setInternalState(underTest, "ambariUserName", "cloudbreak");
        MockitoAnnotations.initMocks(this);
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(100L);
    }

    @Test
    public void testConvert() {
        // GIVEN
        given(conversionService.convert(any(ClusterV4Request.class), eq(Gateway.class))).willReturn(new Gateway());
        // WHEN
        Cluster result = underTest.convert(getRequest("cluster.json"));
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted", "creationFinished", "upSince", "statusReason", "ambariIp",
                "ambariStackDetails", "fileSystem", "certDir", "rdsConfigs", "ldapConfig", "attributes", "blueprintCustomProperties", "uptime",
                "kerberosConfig", "ambariSecurityMasterKey", "proxyConfig", "extendedBlueprintText", "environment", "variant"));
    }

    @Test
    public void testConvertWithFileSystemDetails() {
        // GIVEN
        Gateway gateway = new Gateway();
        given(conversionService.convert(any(ClusterV4Request.class), eq(Gateway.class))).willReturn(gateway);
        given(kerberosService.getByNameForWorkspaceId("somename", 100L)).willReturn(new KerberosConfig());
        given(fileSystemConfigService.getByNameForWorkspaceId("teszt", 100L)).willReturn(new FileSystem());
        // WHEN
        Cluster result = underTest.convert(getRequest("cluster-with-file-system.json"));
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted", "creationFinished", "upSince", "statusReason", "ambariIp",
                "ambariStackDetails", "certDir", "rdsConfigs", "ldapConfig", "attributes", "blueprintCustomProperties", "uptime",
                "ambariSecurityMasterKey", "proxyConfig", "extendedBlueprintText", "environment", "variant"));
    }

    @Test
    public void testNoGateway() {
        // GIVEN
        // WHEN
        ClusterV4Request clusterRequest = getRequest("cluster-no-gateway.json");
        Cluster result = underTest.convert(clusterRequest);
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted", "creationFinished", "upSince", "statusReason", "ambariIp",
                "ambariStackDetails", "fileSystem", "certDir", "rdsConfigs", "ldapConfig", "attributes", "blueprintCustomProperties", "uptime",
                "kerberosConfig", "ambariSecurityMasterKey", "proxyConfig", "extendedBlueprintText", "gateway", "environment", "variant"));
        assertNull(result.getGateway());
    }

    @Override
    public Class<ClusterV4Request> getRequestClass() {
        return ClusterV4Request.class;
    }
}
