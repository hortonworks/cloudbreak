package com.sequenceiq.cloudbreak.service.clusterdefinition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.clusterdefinition.utils.AmbariBlueprintUtils;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.repository.ClusterDefinitionRepository;

@RunWith(MockitoJUnitRunner.class)
public class AmbariBlueprintMigrationServiceTest {
    @InjectMocks
    private AmbariBlueprintMigrationService underTest;

    @Mock
    private ClusterDefinitionRepository clusterDefinitionRepository;

    @Mock
    private AmbariBlueprintUtils ambariBlueprintUtils;

    @Test
    public void testMigrateBlueprintsShouldMigrateStackTypeAndVersion() throws IOException {
        // GIVEN
        List<ClusterDefinition> clusterDefinitions = new ArrayList<>();
        String blueprintJson = readClasspathResourceContent("blueprint-to-migrate.json");
        String wrongBlueprintText = "not a json";
        clusterDefinitions.add(createBlueprint(blueprintJson));
        clusterDefinitions.add(createBlueprint(wrongBlueprintText));
        clusterDefinitions.add(createBlueprint(blueprintJson));
        clusterDefinitions.add(createBlueprint(blueprintJson));
        Mockito.when(clusterDefinitionRepository.findAll()).thenReturn(clusterDefinitions);
        Mockito.when(ambariBlueprintUtils.getBlueprintStackName(Mockito.any()))
                .thenReturn("HDP")
                .thenReturn("")
                .thenReturn("HDF");
        Mockito.when(ambariBlueprintUtils.getBlueprintStackVersion(Mockito.any()))
                .thenReturn("2.6")
                .thenReturn("2.6")
                .thenReturn("");
        ArgumentCaptor<Iterable<ClusterDefinition>> savedBlueprintsIterable = ArgumentCaptor.forClass(Iterable.class);
        // WHEN
        underTest.migrateBlueprints();
        // THEN
        Mockito.verify(clusterDefinitionRepository).saveAll(savedBlueprintsIterable.capture());
        List<ClusterDefinition> savedClusterDefinitions = IteratorUtils.toList(savedBlueprintsIterable.getValue().iterator());
        Assert.assertEquals("HDP", savedClusterDefinitions.get(0).getStackType());
        Assert.assertEquals("2.6", savedClusterDefinitions.get(0).getStackVersion());
        Assert.assertEquals("UNKNOWN", savedClusterDefinitions.get(1).getStackType());
        Assert.assertEquals("UNKNOWN", savedClusterDefinitions.get(1).getStackVersion());
        Assert.assertEquals("UNKNOWN", savedClusterDefinitions.get(2).getStackType());
        Assert.assertEquals("2.6", savedClusterDefinitions.get(2).getStackVersion());
        Assert.assertEquals("HDF", savedClusterDefinitions.get(3).getStackType());
        Assert.assertEquals("UNKNOWN", savedClusterDefinitions.get(3).getStackVersion());
    }

    @Test
    public void testMigrateBlueprintsShouldSkipBlueprintsWithStackTypeAndVersion() throws IOException {
        // GIVEN
        List<ClusterDefinition> clusterDefinitions = new ArrayList<>();
        String blueprintJson = readClasspathResourceContent("blueprint-to-migrate.json");
        clusterDefinitions.add(createBlueprint(blueprintJson, "HDP", "2.7"));
        clusterDefinitions.add(createBlueprint(blueprintJson));
        clusterDefinitions.add(createBlueprint(blueprintJson));
        Mockito.when(clusterDefinitionRepository.findAll()).thenReturn(clusterDefinitions);
        Mockito.when(ambariBlueprintUtils.getBlueprintStackName(Mockito.any()))
                .thenReturn("HDP")
                .thenReturn("HDF");
        Mockito.when(ambariBlueprintUtils.getBlueprintStackVersion(Mockito.any()))
                .thenReturn("2.6")
                .thenReturn("3.2");
        ArgumentCaptor<Iterable<ClusterDefinition>> savedBlueprintsIterable = ArgumentCaptor.forClass(Iterable.class);
        // WHEN
        underTest.migrateBlueprints();
        // THEN
        Mockito.verify(clusterDefinitionRepository).saveAll(savedBlueprintsIterable.capture());
        List<ClusterDefinition> savedClusterDefinitions = IteratorUtils.toList(savedBlueprintsIterable.getValue().iterator());
        Assert.assertEquals("HDP", savedClusterDefinitions.get(0).getStackType());
        Assert.assertEquals("2.6", savedClusterDefinitions.get(0).getStackVersion());
        Assert.assertEquals("HDF", savedClusterDefinitions.get(1).getStackType());
        Assert.assertEquals("3.2", savedClusterDefinitions.get(1).getStackVersion());
    }

    private String readClasspathResourceContent(String resourcePath) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(resourcePath), "UTF-8");
    }

    private ClusterDefinition createBlueprint(String blueprintText, String stackType, String stackVersion) {
        ClusterDefinition clusterDefinition = new ClusterDefinition();
        clusterDefinition.setClusterDefinitionText(blueprintText);
        clusterDefinition.setStackType(stackType);
        clusterDefinition.setStackVersion(stackVersion);
        return clusterDefinition;
    }

    private ClusterDefinition createBlueprint(String blueprintText) {
        return createBlueprint(blueprintText, null, null);
    }
}
