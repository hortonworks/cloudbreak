package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
class CLOServiceRdsConfigProviderTest {

    private static final String BLUEPRINT_WITH_CLO = "{\"cdhVersion\":\"7.3.2\",\"displayName\":\"test\","
            + "\"services\":[{\"refName\":\"clo\",\"serviceType\":\"LAKEHOUSE_OPTIMIZER\",\"roleConfigGroups\":[]}],"
            + "\"hostTemplates\":[]}";

    private static final String BLUEPRINT_WITHOUT_CLO = "{\"cdhVersion\":\"7.3.2\",\"displayName\":\"test\","
            + "\"services\":[{\"refName\":\"hdfs\",\"serviceType\":\"HDFS\",\"roleConfigGroups\":[]}],"
            + "\"hostTemplates\":[]}";

    private static final Long CLUSTER_ID = 1L;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @InjectMocks
    private CLOServiceRdsConfigProvider underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "cmTemplateProcessorFactory", new CmTemplateProcessorFactory());
        ReflectionTestUtils.setField(underTest, "lakehouseOptimizerDbPort", "5432");
        ReflectionTestUtils.setField(underTest, "lakehouseOptimizerDbUser", "clo");
        ReflectionTestUtils.setField(underTest, "lakehouseOptimizerDb", "clo");
    }

    @Test
    void testBasicProperties() {
        assertThat(underTest.getDb()).isEqualTo("clo");
        assertThat(underTest.getDbPort()).isEqualTo("5432");
        assertThat(underTest.getDbUser()).isEqualTo("clo");
        assertThat(underTest.getPillarKey()).isEqualTo("lakehouse_optimizer");
        assertThat(underTest.getRdsType()).isEqualTo(DatabaseType.LAKEHOUSE_OPTIMIZER);
    }

    @Test
    void testIsRdsConfigNeededWhenCloInBlueprintAndVersionSupported() {
        StackDtoDelegate stackDto = createStackDto(BLUEPRINT_WITH_CLO, "7.13.2.10000");

        assertThat(underTest.isRdsConfigNeeded(stackDto)).isTrue();
    }

    @Test
    void testIsRdsConfigNeededWhenCloInBlueprintAndVersionHigher() {
        StackDtoDelegate stackDto = createStackDto(BLUEPRINT_WITH_CLO, "7.13.2.20000");

        assertThat(underTest.isRdsConfigNeeded(stackDto)).isTrue();
    }

    @Test
    void testIsRdsConfigNeededWhenCloInBlueprintButVersionTooLow() {
        StackDtoDelegate stackDto = createStackDto(BLUEPRINT_WITH_CLO, "7.13.1.0");

        assertThat(underTest.isRdsConfigNeeded(stackDto)).isFalse();
    }

    @Test
    void testIsRdsConfigNeededWhenCloNotInBlueprint() {
        StackDtoDelegate stackDto = createStackDto(BLUEPRINT_WITHOUT_CLO, "7.13.2.10000");

        assertThat(underTest.isRdsConfigNeeded(stackDto)).isFalse();
    }

    @Test
    void testIsRdsConfigNeededWhenCloNotInBlueprintAndVersionTooLow() {
        StackDtoDelegate stackDto = createStackDto(BLUEPRINT_WITHOUT_CLO, "7.13.1.0");

        assertThat(underTest.isRdsConfigNeeded(stackDto)).isFalse();
    }

    private StackDtoDelegate createStackDto(String blueprintText, String cmVersion) {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(blueprintText);

        ClusterView cluster = mock(ClusterView.class);
        when(cluster.getId()).thenReturn(CLUSTER_ID);

        StackDtoDelegate stackDto = mock(StackDtoDelegate.class);
        when(stackDto.getBlueprint()).thenReturn(blueprint);
        when(stackDto.getCluster()).thenReturn(cluster);

        ClouderaManagerRepo cmRepo = new ClouderaManagerRepo();
        cmRepo.setVersion(cmVersion);
        when(clusterComponentConfigProvider.getClouderaManagerRepoDetails(CLUSTER_ID)).thenReturn(cmRepo);

        return stackDto;
    }
}
