package com.sequenceiq.cloudbreak.service.blueprint;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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

import com.sequenceiq.cloudbreak.blueprint.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.service.secret.SecretService;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintMigrationServiceTest {
    @InjectMocks
    private BlueprintMigrationService underTest;

    @Mock
    private BlueprintRepository blueprintRepository;

    @Mock
    private BlueprintUtils blueprintUtils;

    @Mock
    private SecretService secretService;

    @Test
    public void testMigrateBlueprintsShouldMigrateStackTypeAndVersion() throws IOException {
        // GIVEN
        List<Blueprint> blueprints = new ArrayList<>();
        String blueprintJson = readClasspathResourceContent("blueprint-to-migrate.json");
        String wrongBlueprintText = "not a json";
        blueprints.add(createBlueprint(blueprintJson));
        blueprints.add(createBlueprint(wrongBlueprintText));
        blueprints.add(createBlueprint(blueprintJson));
        blueprints.add(createBlueprint(blueprintJson));
        Mockito.when(blueprintRepository.findAll()).thenReturn(blueprints);
        Mockito.when(blueprintUtils.getBlueprintStackName(Mockito.any()))
                .thenReturn("HDP")
                .thenReturn("")
                .thenReturn("HDF");
        Mockito.when(blueprintUtils.getBlueprintStackVersion(Mockito.any()))
                .thenReturn("2.6")
                .thenReturn("2.6")
                .thenReturn("");
        ArgumentCaptor<Iterable<Blueprint>> savedBlueprintsIterable = ArgumentCaptor.forClass(Iterable.class);
        when(secretService.get(anyString())).thenAnswer(it -> it.getArgument(0));
        // WHEN
        underTest.migrateBlueprints();
        // THEN
        Mockito.verify(blueprintRepository).saveAll(savedBlueprintsIterable.capture());
        List<Blueprint> savedBlueprints = IteratorUtils.toList(savedBlueprintsIterable.getValue().iterator());
        Assert.assertEquals("HDP", savedBlueprints.get(0).getStackType());
        Assert.assertEquals("2.6", savedBlueprints.get(0).getStackVersion());
        Assert.assertEquals("UNKNOWN", savedBlueprints.get(1).getStackType());
        Assert.assertEquals("UNKNOWN", savedBlueprints.get(1).getStackVersion());
        Assert.assertEquals("UNKNOWN", savedBlueprints.get(2).getStackType());
        Assert.assertEquals("2.6", savedBlueprints.get(2).getStackVersion());
        Assert.assertEquals("HDF", savedBlueprints.get(3).getStackType());
        Assert.assertEquals("UNKNOWN", savedBlueprints.get(3).getStackVersion());
    }

    @Test
    public void testMigrateBlueprintsShouldSkipBlueprintsWithStackTypeAndVersion() throws IOException {
        // GIVEN
        List<Blueprint> blueprints = new ArrayList<>();
        String blueprintJson = readClasspathResourceContent("blueprint-to-migrate.json");
        blueprints.add(createBlueprint(blueprintJson, "HDP", "2.7"));
        blueprints.add(createBlueprint(blueprintJson));
        blueprints.add(createBlueprint(blueprintJson));
        Mockito.when(blueprintRepository.findAll()).thenReturn(blueprints);
        Mockito.when(blueprintUtils.getBlueprintStackName(Mockito.any()))
                .thenReturn("HDP")
                .thenReturn("HDF");
        Mockito.when(blueprintUtils.getBlueprintStackVersion(Mockito.any()))
                .thenReturn("2.6")
                .thenReturn("3.2");
        ArgumentCaptor<Iterable<Blueprint>> savedBlueprintsIterable = ArgumentCaptor.forClass(Iterable.class);
        when(secretService.get(anyString())).thenReturn(blueprintJson);
        // WHEN
        underTest.migrateBlueprints();
        // THEN
        Mockito.verify(blueprintRepository).saveAll(savedBlueprintsIterable.capture());
        List<Blueprint> savedBlueprints = IteratorUtils.toList(savedBlueprintsIterable.getValue().iterator());
        Assert.assertEquals("HDP", savedBlueprints.get(0).getStackType());
        Assert.assertEquals("2.6", savedBlueprints.get(0).getStackVersion());
        Assert.assertEquals("HDF", savedBlueprints.get(1).getStackType());
        Assert.assertEquals("3.2", savedBlueprints.get(1).getStackVersion());
    }

    private String readClasspathResourceContent(String resourcePath) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(resourcePath), "UTF-8");
    }

    private Blueprint createBlueprint(String blueprintText, String stackType, String stackVersion) {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText(blueprintText);
        blueprint.setStackType(stackType);
        blueprint.setStackVersion(stackVersion);
        return blueprint;
    }

    private Blueprint createBlueprint(String blueprintText) {
        return createBlueprint(blueprintText, null, null);
    }
}
