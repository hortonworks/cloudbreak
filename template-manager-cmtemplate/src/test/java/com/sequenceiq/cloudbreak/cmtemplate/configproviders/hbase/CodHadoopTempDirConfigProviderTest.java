package com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@ExtendWith(MockitoExtension.class)
class CodHadoopTempDirConfigProviderTest extends AbstractHbaseConfigProviderTest {

    @InjectMocks
    private final CodHadoopTempDirConfigProvider underTest = new CodHadoopTempDirConfigProvider();

    @Test
    void testHadoopTmpDirForCodCluster() {
        TemplatePreparationObject preparationObject =
                getTemplatePreparationObject(true, false, "7.2.1",
                        Map.of("is_cod_cluster", "true"),
                        CloudPlatform.AWS);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertEquals(1, serviceConfigs.size());
        assertEquals("hadoop.tmp.dir", serviceConfigs.get(0).getName());
        assertEquals("/hadoopfs/fs1/tmp", serviceConfigs.get(0).getValue());
    }

    @Test
    void testNoConfigChangeForNonCodCluster() {
        TemplatePreparationObject preparationObject =
                getTemplatePreparationObject(true, false, "7.2.1");
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertEquals(0, serviceConfigs.size());
    }

    @Test
    void testNoConfigChangeForYcloudCluster() {
        TemplatePreparationObject preparationObject =
                getTemplatePreparationObject(true, false, "7.2.1",
                        Map.of("is_cod_cluster", "true"),
                        CloudPlatform.YARN);
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(cmTemplateProcessor, preparationObject);

        assertEquals(0, serviceConfigs.size());
    }
}
