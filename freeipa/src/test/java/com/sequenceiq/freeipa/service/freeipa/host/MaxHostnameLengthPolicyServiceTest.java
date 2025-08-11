package com.sequenceiq.freeipa.service.freeipa.host;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.model.Config;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@ExtendWith(MockitoExtension.class)
class MaxHostnameLengthPolicyServiceTest {

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Spy
    private Rhel8ClientHelper rhel8ClientHelper;

    @InjectMocks
    private MaxHostnameLengthPolicyService underTest;

    @Test
    public void testMaxHostnameConfigModInvokedWhenTheConfiguredValueLessThenTheRequiredByMaxFQDNLength() throws Exception {
        Stack stack = mock(Stack.class);
        InstanceMetaData gatewayInstanceMetaData = new InstanceMetaData();
        gatewayInstanceMetaData.setDiscoveryFQDN("shorthostname.domainpart1.domainpart2.clouder.site");
        Image rhel8 = createImageWithOs(OsType.RHEL8);
        gatewayInstanceMetaData.setImage(new Json(rhel8));
        when(stack.getPrimaryGateway()).thenReturn(Optional.of(gatewayInstanceMetaData));
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(gatewayInstanceMetaData));
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        when(ipaClient.getHostname()).thenReturn("shorthostname.domainpart1.domainpart2.clouder.site");
        Config ipaConfig = new Config();
        ipaConfig.setIpamaxusernamelength(5);
        ipaConfig.setIpamaxhostnamelength(64);
        when(ipaClient.getConfig()).thenReturn(ipaConfig);

        underTest.updateMaxHostnameLength(stack, ipaClient);

        verify(ipaClient).setMaxHostNameLength(100);
        verifyNoInteractions(freeIpaClientFactory);
    }

    @Test
    public void testMaxHostnameConfigModInvokedWhenTheConfiguredValueLowAndDomainExtremelyLong() throws Exception {
        Stack stack = mock(Stack.class);
        InstanceMetaData gatewayInstanceMetaData = new InstanceMetaData();
        gatewayInstanceMetaData.setDiscoveryFQDN("shorthostname.butaveryveryverylong.domainbuthatismoremorelongthanwhatisexpected." +
                "domainbuthatismoremorelongthanwhatisexpected.domainbuthatismoremorelongthanwhatisexpected.domainbuthatismoremorelongthanwhatisexpected");
        Image rhel8 = createImageWithOs(OsType.RHEL8);
        gatewayInstanceMetaData.setImage(new Json(rhel8));
        when(stack.getPrimaryGateway()).thenReturn(Optional.of(gatewayInstanceMetaData));
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(gatewayInstanceMetaData));
        FreeIpaClient ipaClient = mock(FreeIpaClient.class);
        when(ipaClient.getHostname()).thenReturn("shorthostname.butaveryveryverylong.domainbuthatismoremorelongthanwhatisexpected." +
                "domainbuthatismoremorelongthanwhatisexpected.domainbuthatismoremorelongthanwhatisexpected.domainbuthatismoremorelongthanwhatisexpected");
        Config ipaConfig = new Config();
        ipaConfig.setIpamaxusernamelength(5);
        ipaConfig.setIpamaxhostnamelength(64);
        when(ipaClient.getConfig()).thenReturn(ipaConfig);

        underTest.updateMaxHostnameLength(stack, ipaClient);

        verify(ipaClient).setMaxHostNameLength(255);
        verifyNoInteractions(freeIpaClientFactory);
    }

    @Test
    public void testMaxHostnameConfigModInvokedWhenPgwIsNotRhel8() throws Exception {
        Stack stack = mock(Stack.class);
        InstanceMetaData gatewayInstanceMetaData = new InstanceMetaData();
        gatewayInstanceMetaData.setDiscoveryFQDN("shorthostname.domainpart1.domainpart2.clouder.site");
        Image centos = createImageWithOs(OsType.CENTOS7);
        gatewayInstanceMetaData.setImage(new Json(centos));
        InstanceMetaData rhel8Instance = new InstanceMetaData();
        rhel8Instance.setDiscoveryFQDN("rhel8.domainpart1.domainpart2.clouder.site");
        Image rhel8Image = createImageWithOs(OsType.RHEL8);
        rhel8Instance.setImage(new Json(rhel8Image));
        when(stack.getPrimaryGateway()).thenReturn(Optional.of(gatewayInstanceMetaData));
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(gatewayInstanceMetaData, rhel8Instance));
        FreeIpaClient centosClient = mock(FreeIpaClient.class);
        when(centosClient.getHostname()).thenReturn("shorthostname.domainpart1.domainpart2.clouder.site");
        Config ipaConfig = new Config();
        ipaConfig.setIpamaxusernamelength(5);
        ipaConfig.setIpamaxhostnamelength(64);
        when(centosClient.getConfig()).thenReturn(ipaConfig);
        FreeIpaClient rhel8Client = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForInstance(stack, "rhel8.domainpart1.domainpart2.clouder.site")).thenReturn(rhel8Client);

        underTest.updateMaxHostnameLength(stack, centosClient);

        verify(rhel8Client).setMaxHostNameLength(100);
    }

    @Test
    public void testMaxHostnameConfigModInvokedWhenPgwImageHasNoContent() throws Exception {
        Stack stack = mock(Stack.class);
        InstanceMetaData gatewayInstanceMetaData = new InstanceMetaData();
        gatewayInstanceMetaData.setDiscoveryFQDN("shorthostname.domainpart1.domainpart2.clouder.site");
        gatewayInstanceMetaData.setImage(new Json(null));
        InstanceMetaData rhel8Instance = new InstanceMetaData();
        rhel8Instance.setDiscoveryFQDN("rhel8.domainpart1.domainpart2.clouder.site");
        Image rhel8Image = createImageWithOs(OsType.RHEL8);
        rhel8Instance.setImage(new Json(rhel8Image));
        when(stack.getPrimaryGateway()).thenReturn(Optional.of(gatewayInstanceMetaData));
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(gatewayInstanceMetaData, rhel8Instance));
        FreeIpaClient centosClient = mock(FreeIpaClient.class);
        when(centosClient.getHostname()).thenReturn("shorthostname.domainpart1.domainpart2.clouder.site");
        Config ipaConfig = new Config();
        ipaConfig.setIpamaxusernamelength(5);
        ipaConfig.setIpamaxhostnamelength(64);
        when(centosClient.getConfig()).thenReturn(ipaConfig);
        FreeIpaClient rhel8Client = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForInstance(stack, "rhel8.domainpart1.domainpart2.clouder.site")).thenReturn(rhel8Client);

        underTest.updateMaxHostnameLength(stack, centosClient);

        verify(rhel8Client).setMaxHostNameLength(100);
    }

    @Test
    public void testMaxHostnameConfigModInvokedWhenPgwIsNotRhel8NoRhel8Available() throws Exception {
        Stack stack = mock(Stack.class);
        InstanceMetaData gatewayInstanceMetaData = new InstanceMetaData();
        gatewayInstanceMetaData.setDiscoveryFQDN("shorthostname.domainpart1.domainpart2.clouder.site");
        Image centos = createImageWithOs(OsType.CENTOS7);
        gatewayInstanceMetaData.setImage(new Json(centos));
        InstanceMetaData anotherInstance = new InstanceMetaData();
        anotherInstance.setDiscoveryFQDN("hmm.domainpart1.domainpart2.clouder.site");
        anotherInstance.setImage(new Json(centos));
        when(stack.getPrimaryGateway()).thenReturn(Optional.of(gatewayInstanceMetaData));
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(gatewayInstanceMetaData, anotherInstance));
        FreeIpaClient centosClient = mock(FreeIpaClient.class);
        when(centosClient.getHostname()).thenReturn("shorthostname.domainpart1.domainpart2.clouder.site");
        Config ipaConfig = new Config();
        ipaConfig.setIpamaxusernamelength(5);
        ipaConfig.setIpamaxhostnamelength(64);
        when(centosClient.getConfig()).thenReturn(ipaConfig);

        underTest.updateMaxHostnameLength(stack, centosClient);

        verifyNoInteractions(freeIpaClientFactory);
        verify(centosClient, never()).setMaxHostNameLength(anyInt());
    }

    private Image createImageWithOs(OsType osType) {
        return new Image(null, null, osType.getOs(), osType.getOsType(), null, null, null, null, null, null, null, null);
    }
}