package com.sequenceiq.cloudbreak.service.cluster.flow.kerberos;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.services.KerberosService;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class KerberosBlueprintServiceTest {

    @Mock
    private Stack stack;

    @Spy
    private KerberosDetailService kerberosDetailService;

    @InjectMocks
    private KerberosBlueprintService underTest;

    private final KerberosService kerberosService = new AmbariClient();

    @Before
    public void setup() {
        InstanceGroup instanceGroup = TestUtil.instanceGroup(1L, InstanceGroupType.GATEWAY, null);
        when(stack.getPrimaryGatewayInstance()).thenReturn(TestUtil.instanceMetaData(1L, 1L, InstanceStatus.REGISTERED, true, instanceGroup));
        when(stack.getInstanceGroups()).thenReturn(Collections.singleton(instanceGroup));
    }

    @Test
    public void testExtendBlueprintWithKerberosOnKerberizedBlueprint() throws IOException {
        String expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/bp-kerberized-test.bp");
        Blueprint blueprint = TestUtil.blueprint("name", expectedBlueprint);
        when(stack.getCluster()).thenReturn(TestUtil.cluster(blueprint, stack, 1L, TestUtil.kerberosConfig()));

        String actualBlueprint = underTest.extendBlueprintWithKerberos(stack, blueprint.getBlueprintText(), kerberosService);

        Assert.assertEquals(expectedBlueprint, actualBlueprint);
    }

    @Test
    public void testExtendBlueprintWithKerberosManagedKerberos() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints/bp-not-kerberized.bp");
        Blueprint blueprint = TestUtil.blueprint("name", blueprintText);
        when(stack.getCluster()).thenReturn(TestUtil.cluster(blueprint, stack, 1L, TestUtil.kerberosConfig()));

        String actualBlueprint = underTest.extendBlueprintWithKerberos(stack, blueprint.getBlueprintText(), kerberosService);

        String expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/bp-not-kerberized-cloudbreak-managed-expected.bp");
        Assert.assertEquals(expectedBlueprint, actualBlueprint);
    }

    @Test
    public void testExtendBlueprintWithKerberosCustomConfig() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints/bp-not-kerberized.bp");
        Blueprint blueprint = TestUtil.blueprint("name", blueprintText);
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setKerberosDescriptor("{\"kerberos-env\":{\"properties\":{\"install_packages\":false,\"realm\":\"REALM.BP\",\"kdc_type\":\"mit-kdc\","
            + "\"kdc_hosts\":\"kdc_host.bp\",\"admin_server_host\":\"admin_server_host.bp\",\"encryption_types\":\"enc_types.bp\",\"ldap_url\":\"\","
            + "\"container_dn\":\"\"}}}");
        kerberosConfig.setKrb5Conf("{\"krb5-conf\":{\"properties\":{\"domains\":\".domains.bp\",\"manage_krb5_conf\":\"true\",\"content\":\"content.bp\"}}}");
        kerberosConfig.setKerberosTcpAllowed(true);
        when(stack.getCluster()).thenReturn(TestUtil.cluster(blueprint, stack, 1L, kerberosConfig));

        String actualBlueprint = underTest.extendBlueprintWithKerberos(stack, blueprint.getBlueprintText(), kerberosService);

        String expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/bp-not-kerberized-custom-config-expected.bp");
        Assert.assertEquals(expectedBlueprint, actualBlueprint);
    }

    @Test
    public void testExtendBlueprintWithKerberosExisting() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints/bp-not-kerberized.bp");
        Blueprint blueprint = TestUtil.blueprint("name", blueprintText);
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setKerberosPrincipal("principal.conf");
        kerberosConfig.setKerberosPrincipal("passwd.conf");
        kerberosConfig.setKerberosUrl("url.conf");
        kerberosConfig.setKdcAdminUrl("adminUrl.conf");
        kerberosConfig.setKerberosRealm("realm.conf");
        kerberosConfig.setKerberosLdapUrl("ldapUrl.conf");
        kerberosConfig.setKerberosContainerDn("containerDn.conf");
        kerberosConfig.setKerberosTcpAllowed(true);
        when(stack.getCluster()).thenReturn(TestUtil.cluster(blueprint, stack, 1L, kerberosConfig));

        String actualBlueprint = underTest.extendBlueprintWithKerberos(stack, blueprint.getBlueprintText(), kerberosService);

        String expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/bp-not-kerberized-existing-expected.bp");
        Assert.assertEquals(expectedBlueprint, actualBlueprint);
    }
}