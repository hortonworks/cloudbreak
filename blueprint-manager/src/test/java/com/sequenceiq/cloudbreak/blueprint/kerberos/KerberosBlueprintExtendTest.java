package com.sequenceiq.cloudbreak.blueprint.kerberos;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.template.processor.processor.TemplateProcessorFactory;
import com.sequenceiq.cloudbreak.template.processor.processor.TemplateTextProcessor;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class KerberosBlueprintExtendTest {

    private static ImmutableMap<String, String> kerberosEnv;

    @Spy
    private TemplateProcessorFactory blueprintProcessorFactory;

    @InjectMocks
    @Spy
    private KerberosBlueprintService underTest;

    @BeforeClass
    public static void setUp() {
        kerberosEnv = ImmutableMap.<String, String>builder()
                .put("realm", "NODE.DC1.CONSUL")
                .put("kdc_type", "mit-kdc")
                .put("kdc_hosts", "hostname.node.dc1.consul")
                .put("admin_server_host", "hostname1.node.dc1.consul")
                .put("encryption_types", "aes des3-cbc-sha1 rc4 des-cbc-md5")
                .put("ldap_url", "")
                .put("container_dn", "")
                .build();
    }

    @Test
    public void testAddKerberosConfigToClearBlueprint() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-kerb-forced.json");

        TemplateTextProcessor b = new TemplateTextProcessor(json);
        String result = underTest.extendBlueprintWithKerberos(b, kerberosEnv, "node.dc1.consul,node.consul", true, null, false).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testAddKerberosConfigTokerverosConfiguredBlueprintNotForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-default-kerb.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-kerb.json");

        TemplateTextProcessor b = new TemplateTextProcessor(json);
        String result = underTest.extendBlueprintWithKerberos(b, kerberosEnv, "node.dc1.consul,node.consul", true, null, false).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testAddKerberosConfigTokerverosConfiguredBlueprintForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-default-kerb.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-kerb-forced.json");

        TemplateTextProcessor b = new TemplateTextProcessor(json);
        String result = underTest.extendBlueprintWithKerberos(b, kerberosEnv, "node.dc1.consul,node.consul", true, null, true).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testAddKerberosConfigToKerberosConfiguredBlueprintWithDescriptorNotForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-default-kerb_descriptor.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-default-kerb_descriptor_fixed.json");

        TemplateTextProcessor b = new TemplateTextProcessor(json);
        String result = underTest.extendBlueprintWithKerberos(b, kerberosEnv, "node.dc1.consul,node.consul", false, null, false).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testAddKerberosConfigToKerberosConfiguredBlueprintWithDescriptorForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-default-kerb_descriptor.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-default-kerb_descriptor_forced_fixed.json");

        TemplateTextProcessor b = new TemplateTextProcessor(json);
        String result = underTest.extendBlueprintWithKerberos(b, kerberosEnv, "node.dc1.consul,node.consul", false, null, true).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

}
