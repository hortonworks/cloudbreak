package com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles.KNOX;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockitoAnnotations;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;

public class KnoxServiceConfigProviderTest {

    private KnoxServiceConfigProvider underTest = new KnoxServiceConfigProvider();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetServiceTypeShouldReturnKnox() {
        Assert.assertTrue(underTest.getServiceType().equals(KNOX));
    }

    @Test
    public void testGetRoleConfigsShouldReturnEmptyList() {
        Assert.assertTrue(underTest.getRoleConfigs("", null).isEmpty());
    }

    @Test
    public void testDbTypeShouldReturnKnoxGateway() {
        Assert.assertTrue(underTest.dbType().equals(DatabaseType.KNOX_GATEWAY));
    }

    @Test
    public void testGetRoleTypesShouldReturnKnoxGatewayIdbroker() {
        Assert.assertTrue(underTest.getRoleTypes().equals(List.of(KnoxRoles.KNOX_GATEWAY, KnoxRoles.IDBROKER)));
    }

    @ParameterizedTest(name = "{index}: check knox properties cm version {0} and cdh version {1} will produce {2} property")
    @MethodSource("cmCdhCombinations")
    public void testGetServiceConfigsWhenCMAtLeast741AndCDHVersion7291ShouldIncludeDBProperties(
            String cdhVersion,
            String cmVersion,
            int numberOfProperties) {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        BlueprintView blueprintView = new BlueprintView("text", cdhVersion, "CDH", blueprintTextProcessor);
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setConnectionPassword("pw");
        rdsConfig.setConnectionUserName("usr");
        rdsConfig.setType(DatabaseType.KNOX_GATEWAY.name());
        rdsConfig.setConnectionURL("jdbc:postgresql://somehost.com:5432/dbName");
        TemplatePreparationObject source = TemplatePreparationObject.Builder.builder()
                .withBlueprintView(blueprintView)
                .withRdsSslCertificateFilePath("file://path")
                .withRdsConfigs(Set.of(rdsConfig))
                .withProductDetails(new ClouderaManagerRepo().withVersion(cmVersion), List.of())
                .build();

        when(blueprintTextProcessor.getStackVersion()).thenReturn(cdhVersion);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, source);

        Assert.assertTrue(serviceConfigs.size() == numberOfProperties);
    }

    static Object[][] cmCdhCombinations() {
        return new Object[][]{
                { "7.2.10",     "7.4.2", 7 },
                { "7.2.11",     "7.4.1", 1 },
                { "7.2.11",     "7.4.2", 7 },
                { "7.2.9.1",    "7.4.1", 7 },
                { "7.2.8",      "7.4.2", 1 },
                { "7.2.8",      "7.4.1", 1 },
                { "7.2.9",      "7.4.2", 1 },
        };
    }
}