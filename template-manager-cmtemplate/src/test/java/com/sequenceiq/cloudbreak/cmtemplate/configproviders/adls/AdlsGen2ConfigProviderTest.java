package com.sequenceiq.cloudbreak.cmtemplate.configproviders.adls;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.filesystem.adlsgen2.AdlsGen2FileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.template.views.PlacementView;
import com.sequenceiq.common.api.filesystem.AdlsGen2FileSystem;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class AdlsGen2ConfigProviderTest {

    @InjectMocks
    private AdlsGen2ConfigProvider underTest;

    @Test
    void testPopulateServiceConfigsWithAdlsGen2FileSystemConfiguredPost7212() {
        StringBuilder sb = new StringBuilder();
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(true);

        underTest.populateServiceConfigs(preparationObject, sb, "7.2.15");

        assertThat(sb).hasToString("<property><name>fs.azure.enable.readahead</name><value>false</value></property>");
    }

    @Test
    void testPopulateServiceConfigsWithoutAdlsGen2FileSystemConfiguredPost7212() {
        StringBuilder sb = new StringBuilder();
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(false);

        underTest.populateServiceConfigs(preparationObject, sb, "7.2.15");

        assertThat(sb).isBlank();
    }

    @Test
    void testPopulateServiceConfigsWithAdlsGen2FileSystemConfiguredPre7212() {
        StringBuilder sb = new StringBuilder();
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(false);

        underTest.populateServiceConfigs(preparationObject, sb, "7.2.11");

        assertThat(sb).isBlank();
    }

    private TemplatePreparationObject getTemplatePreparationObject(boolean useAdlsGen2) {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        List<StorageLocationView> locations = new ArrayList<>();

        BaseFileSystemConfigurationsView fileSystemConfigurationsView = null;

        if (useAdlsGen2) {
            fileSystemConfigurationsView = new AdlsGen2FileSystemConfigurationsView(new AdlsGen2FileSystem(), locations, false);
        }

        Gateway gateway = TestUtil.gatewayEnabledWithExposedKnoxServices("NAMENODE");

        PlacementView placementView = new PlacementView("region", "az");

        return TemplatePreparationObject.Builder.builder().withFileSystemConfigurationView(fileSystemConfigurationsView)
                .withHostgroupViews(Set.of(master, worker))
                .withGateway(gateway, "/cb/secret/signkey", new HashSet<>())
                .withPlacementView(placementView)
                .withDefaultTags(Map.of("apple", "apple1"))
                .build();
    }
}