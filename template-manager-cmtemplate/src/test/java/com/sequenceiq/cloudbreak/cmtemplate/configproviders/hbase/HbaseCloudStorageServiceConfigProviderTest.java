package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@RunWith(MockitoJUnitRunner.class)
public class HbaseCloudStorageServiceConfigProviderTest extends AbstractHbaseConfigProviderTest {

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @InjectMocks
    private final HbaseCloudStorageServiceConfigProvider underTest = new HbaseCloudStorageServiceConfigProvider();

    @Mock
    private EntitlementService entitlementService;

    @Before
    public void setUp() {
        when(entitlementService.sdxHbaseCloudStorageEnabled(anyString())).thenReturn(true);
    }

    @Test
    public void testGetHbaseStorageServiceConfigsWhenAttachedCluster() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, false);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("hdfs_rootdir", serviceConfigs.get(0).getName());
        assertEquals("s3a://bucket/cluster1/hbase", serviceConfigs.get(0).getValue());
    }

    @Test
    public void testGetHbaseStorageServiceConfigsWhenDataLake721() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, true, "7.2.1");
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("hdfs_rootdir", serviceConfigs.get(0).getName());
        assertEquals("s3a://bucket/cluster1/hbase", serviceConfigs.get(0).getValue());
    }

    @Test
    public void testGetHbaseStorageServiceConfigsWhenDataLake722() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, true, "7.2.2");
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("hdfs_rootdir", serviceConfigs.get(0).getName());
        assertEquals("s3a://bucket/cluster1/hbase", serviceConfigs.get(0).getValue());
    }

    @Test
    public void testGetHbaseServiceConfigsWhenNoStorageConfiguredWithAttachedCluster() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(false, false);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);
        assertEquals(0, serviceConfigs.size());
    }

    @Test
    public void testGetHbaseServiceConfigsWhenNoStorageConfiguredWithDataLake() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(false, true);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);
        assertEquals(0, serviceConfigs.size());
    }

    @Test
    public void testConfigurationNotNeededWhenDataLake721() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, true, "7.2.1");
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        boolean configurationNeeded = testIsConfigurationNeeded(preparationObject, cmTemplateProcessor);
        assertFalse(configurationNeeded);
    }

    @Test
    public void testConfigurationNotNeededWhenDataLake726() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, true, "7.2.6");
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        boolean configurationNeeded = testIsConfigurationNeeded(preparationObject, cmTemplateProcessor);
        assertFalse(configurationNeeded);
    }

    @Test
    public void testConfigurationNeededWhenDatalake727() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, true, "7.2.7");
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        boolean configurationNeeded = testIsConfigurationNeeded(preparationObject, cmTemplateProcessor);
        assertTrue(configurationNeeded);
    }

    @Test
    public void testConfigurationNeededWhenDatalake728() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, true, "7.2.8");
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        boolean configurationNeeded = testIsConfigurationNeeded(preparationObject, cmTemplateProcessor);
        assertTrue(configurationNeeded);
    }

    @Test
    public void testIsConfigurationNeededWhenAttachedCluster() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true, false);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        boolean configurationNeeded = testIsConfigurationNeeded(preparationObject, cmTemplateProcessor);
        assertTrue(configurationNeeded);
    }

    private boolean testIsConfigurationNeeded(TemplatePreparationObject preparationObject, CmTemplateProcessor cmTemplateProcessor) {
        return ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject));
    }
}
