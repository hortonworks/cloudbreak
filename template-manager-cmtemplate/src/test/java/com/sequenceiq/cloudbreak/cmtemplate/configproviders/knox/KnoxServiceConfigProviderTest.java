package com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles.KNOX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.RdsView;
import com.sequenceiq.cloudbreak.util.CdhPatchVersionProvider;

@ExtendWith(MockitoExtension.class)
public class KnoxServiceConfigProviderTest {

    @Mock
    private CmTemplateProcessor cmTemplate;

    @InjectMocks
    private KnoxServiceConfigProvider underTest;

    @Spy
    private CdhPatchVersionProvider cdhPatchVersionProvider;

    @Test
    public void testGetServiceTypeShouldReturnKnox() {
        assertEquals(KNOX, underTest.getServiceType());
    }

    @ParameterizedTest(name = "{index}: check knox properties cm version {0} and cdh version {1} will produce {2} property")
    @MethodSource("cmCdhCombinationsRoleConfigs")
    public void testGetRoleConfigsShouldReturnEmptyList(String cdhVersion, String cmVersion, boolean ssl,
        int numberOfProperties, boolean sslPresented) {
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        BlueprintView blueprintView = new BlueprintView("text", cdhVersion, "CDH", null, blueprintTextProcessor);
        RdsView rdsConfig = mock(RdsView.class);
        lenient().when(rdsConfig.isUseSsl()).thenReturn(ssl);
        when(rdsConfig.getType()).thenReturn(DatabaseType.KNOX_GATEWAY.name());
        lenient().when(rdsConfig.getConnectionURL()).thenReturn("jdbc:postgresql://somehost.com:5432/dbName");
        TemplatePreparationObject source = TemplatePreparationObject.Builder.builder()
                .withBlueprintView(blueprintView)
                .withRdsSslCertificateFilePath("file://path")
                .withRdsViews(Set.of(rdsConfig))
                .withProductDetails(new ClouderaManagerRepo().withVersion(cmVersion),
                        List.of(new ClouderaManagerProduct()
                                .withVersion(cdhVersion)
                                .withName("CDH")))
                .build();
        List<ApiClusterTemplateConfig> roleConfigs = underTest.getRoleConfigs("", cmTemplate, source);

        Optional<ApiClusterTemplateConfig> sslOptional = roleConfigs
                .stream()
                .filter(e -> e.getName().equals("gateway_database_ssl_enabled"))
                .findFirst();
        if (sslPresented) {
            assertTrue(sslOptional.isPresent());
        } else {
            assertTrue(sslOptional.isEmpty());
        }
        assertEquals(numberOfProperties, roleConfigs.size());
    }

    @Test
    public void testDbTypeShouldReturnKnoxGateway() {
        assertEquals(DatabaseType.KNOX_GATEWAY, underTest.dbType());
    }

    @Test
    public void testGetRoleTypesShouldReturnKnoxGatewayIdbroker() {
        assertEquals(List.of(KnoxRoles.KNOX_GATEWAY, KnoxRoles.IDBROKER), underTest.getRoleTypes());
    }

    @ParameterizedTest(name = "{index}: check knox properties cm version {0} and cdh version {1} will produce {2} property")
    @MethodSource("cmCdhCombinations")
    public void testGetServiceConfigsWhenCMAtLeast741AndCDHVersion7291ShouldIncludeDBProperties(
            String cdhVersion,
            String cmVersion,
            int numberOfProperties) {
        CmTemplateProcessor templateProcessor = mock(CmTemplateProcessor.class);
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        BlueprintView blueprintView = new BlueprintView("text", cdhVersion, "CDH", null, blueprintTextProcessor);
        RdsView rdsConfig = mock(RdsView.class);
        when(rdsConfig.getType()).thenReturn(DatabaseType.KNOX_GATEWAY.name());
        TemplatePreparationObject source = TemplatePreparationObject.Builder.builder()
                .withBlueprintView(blueprintView)
                .withRdsSslCertificateFilePath("file://path")
                .withRdsViews(Set.of(rdsConfig))
                .withProductDetails(new ClouderaManagerRepo().withVersion(cmVersion),
                        List.of(new ClouderaManagerProduct()
                                .withVersion(cdhVersion)
                                .withName("CDH")))
                .build();

        lenient().when(blueprintTextProcessor.getStackVersion()).thenReturn(cdhVersion);

        List<ApiClusterTemplateConfig> serviceConfigs = underTest.getServiceConfigs(templateProcessor, source);

        assertEquals(numberOfProperties, serviceConfigs.size());
    }

    static Object[][] cmCdhCombinations() {
        return new Object[][]{
                { "7.2.10",                         "7.4.2", 7 },
                { "7.2.11",                         "7.4.1", 1 },
                { "7.2.11",                         "7.4.2", 7 },
                { "7.2.9-1.cdh7.2.9.p1.14166188",   "7.4.1", 7 },
                { "7.2.8",                          "7.4.2", 1 },
                { "7.2.8",                          "7.4.1", 1 },
                { "7.2.9",                          "7.4.2", 1 },
        };
    }

    static Object[][] cmCdhCombinationsRoleConfigs() {
        return new Object[][]{
                { "7.2.10",                         "7.4.2", true,   2, true },
                { "7.2.11",                         "7.4.1", true,   0, false },
                { "7.2.11",                         "7.4.2", true,   2, true },
                { "7.2.9-1.cdh7.2.9.p1.14166188",   "7.4.1", true,   2, true },
                { "7.2.8",                          "7.4.2", true,   0, false },
                { "7.2.8",                          "7.4.1", true,   0, false },
                { "7.2.9",                          "7.4.2", true,   0, false },
                { "7.2.10",                         "7.4.2", false,  0, false },
                { "7.2.11",                         "7.4.1", false,  0, false },
                { "7.2.11",                         "7.4.2", false,  0, false },
                { "7.2.9-1.cdh7.2.9.p1.14166188",   "7.4.1", false,  0, false },
                { "7.2.8",                          "7.4.2", false,  0, false },
                { "7.2.8",                          "7.4.1", false,  0, false },
                { "7.2.9",                          "7.4.2", false,  0, false },
        };
    }
}