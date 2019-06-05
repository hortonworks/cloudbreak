package com.sequenceiq.cloudbreak.blueprint.kerberos;

import java.io.IOException;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.blueprint.filesystem.BlueprintTestUtil;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class KerberosBlueprintServiceTest {

    @Spy
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @Mock
    private TemplatePreparationObject templatePreparationObject;

    @Spy
    private KerberosDetailService kerberosDetailService;

    @InjectMocks
    private KerberosBlueprintService underTest;

    @Test
    public void testExtendBlueprintWithKerberosOnKerberizedBlueprint() throws IOException {
        String expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        Blueprint blueprint = TestUtil.blueprint("name", expectedBlueprint);
        TemplatePreparationObject object = Builder.builder()
                .withKerberosConfig(TestUtil.kerberosConfigFreeipa())
                .withGeneralClusterConfigs(BlueprintTestUtil.generalClusterConfigs())
                .build();

        AmbariBlueprintTextProcessor b = new AmbariBlueprintTextProcessor(blueprint.getBlueprintText());
        String actualBlueprint = underTest.customTextManipulation(object, b).asText();

        JsonNode expectedNode = JsonUtil.readTree(expectedBlueprint);
        JsonNode resultNode = JsonUtil.readTree(actualBlueprint);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintWithKerberosCustomConfig() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-not-kerberized.bp");

        Blueprint blueprint = TestUtil.blueprint("name", blueprintText);
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
            .withDescriptor("{\"kerberos-env\":{\"properties\":"
                + "{\"install_packages\":false,\"realm\":\"REALM.BP\",\"kdc_type\":\"mit-kdc\","
                + "\"kdc_hosts\":\"kdc_host.bp\",\"admin_server_host\":\"admin_server_host.bp\",\"encryption_types\":\"enc_types.bp\",\"ldap_url\":\"\","
                + "\"container_dn\":\"\"}}}")
            .withKrb5Conf("{\"krb5-conf\":{\"properties\":{\"domains\":\".domains.bp\","
                + "\"manage_krb5_conf\":\"true\",\"content\":\"content.bp\"}}}")
            .withTcpAllowed(true)
            .withType(KerberosType.MIT)
            .build();
        TemplatePreparationObject object = Builder.builder()
                .withKerberosConfig(kerberosConfig)
                .withGeneralClusterConfigs(BlueprintTestUtil.generalClusterConfigs())
                .build();

        AmbariBlueprintTextProcessor b = new AmbariBlueprintTextProcessor(blueprint.getBlueprintText());
        String actualBlueprint = underTest.customTextManipulation(object, b).asText();

        String expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-not-kerberized-custom-config-expected.bp");

        JsonNode expectedNode = JsonUtil.readTree(expectedBlueprint);
        JsonNode resultNode = JsonUtil.readTree(actualBlueprint);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintWithKerberosExisting() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-not-kerberized.bp");

        Blueprint blueprint = TestUtil.blueprint("name", blueprintText);
        KerberosConfig kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
            .withPrincipal("principal.conf")
            .withPassword("passwd.conf")
            .withUrl("url.conf")
            .withAdminUrl("adminUrl.conf")
            .withRealm("realm.conf")
            .withLdapUrl("ldapUrl.conf")
            .withContainerDn("containerDn.conf")
            .withTcpAllowed(true)
            .withType(KerberosType.ACTIVE_DIRECTORY)
            .build();

        GeneralClusterConfigs generalClusterConfigs = BlueprintTestUtil.generalClusterConfigs();
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of("test-1-1"));
        generalClusterConfigs.setGatewayInstanceMetadataPresented(false);

        TemplatePreparationObject object = Builder.builder()
                .withKerberosConfig(kerberosConfig)
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
        AmbariBlueprintTextProcessor b = new AmbariBlueprintTextProcessor(blueprint.getBlueprintText());
        String actualBlueprint = underTest.customTextManipulation(object, b).asText();

        String expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-not-kerberized-existing-expected.bp");

        JsonNode expectedNode = JsonUtil.readTree(expectedBlueprint);
        JsonNode resultNode = JsonUtil.readTree(actualBlueprint);
        Assert.assertEquals(expectedNode, resultNode);
    }
}