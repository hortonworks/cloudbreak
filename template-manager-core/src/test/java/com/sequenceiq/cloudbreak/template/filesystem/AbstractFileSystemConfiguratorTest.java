package com.sequenceiq.cloudbreak.template.filesystem;

import static com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil.adlsFileSystem;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.model.recipe.RecipeScript;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.common.type.ExecutionType;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.template.filesystem.adls.AdlsFileSystemConfigurationsView;

class AbstractFileSystemConfiguratorTest {

    private final AbstractFileSystemConfiguratorImpl underTest = new AbstractFileSystemConfiguratorImpl();

    @Test
    void testGetScripts() {
        Credential credential = Credential.builder().crn("crn0").build();
        AdlsFileSystemConfigurationsView adlsFileSystemConfigurationsView = new AdlsFileSystemConfigurationsView(adlsFileSystem(), new HashSet<>(), false);
        List<RecipeScript> actual = underTest.getScripts(credential, adlsFileSystemConfigurationsView);

        List<RecipeScript> expected = singletonList(new RecipeScript("echo 'newContent'", ExecutionType.ALL_NODES, RecipeType.POST_CLOUDERA_MANAGER_START));
        assertEquals(expected, actual);
    }

    @Test
    void testGetScriptsWhenNoReplace() {
        Credential credential = Credential.builder().crn("crn1").build();
        AdlsFileSystemConfigurationsView adlsFileSystemConfigurationsView = new AdlsFileSystemConfigurationsView(adlsFileSystem(), new HashSet<>(), false);
        List<RecipeScript> actual = underTest.getScripts(credential, adlsFileSystemConfigurationsView);

        List<RecipeScript> expected = singletonList(new RecipeScript("echo '$replace'", ExecutionType.ALL_NODES, RecipeType.POST_CLOUDERA_MANAGER_START));
        assertEquals(expected, actual);
    }

    @Test
    void testGetScriptsWhenFileNotFound() {
        Credential credential = Credential.builder().crn("crn2").build();
        AdlsFileSystemConfigurationsView adlsFileSystemConfigurationsView = new AdlsFileSystemConfigurationsView(adlsFileSystem(), new HashSet<>(), false);

        assertThrows(FileSystemConfigException.class, () -> underTest.getScripts(credential, adlsFileSystemConfigurationsView),
                "Filesystem configuration scripts cannot be read.");
    }

    @Test
    void testCreateResource() {
        AdlsFileSystemConfigurationsView adlsFileSystemConfigurationsView = new AdlsFileSystemConfigurationsView(adlsFileSystem(), new HashSet<>(), false);

        Map<String, String> actual = underTest.createResources(adlsFileSystemConfigurationsView);

        assertEquals(emptyMap(), actual);
    }

    @Test
    void testDelteResource() {
        AdlsFileSystemConfigurationsView adlsFileSystemConfigurationsView = new AdlsFileSystemConfigurationsView(adlsFileSystem(), new HashSet<>(), false);

        Map<String, String> actual = underTest.deleteResources(adlsFileSystemConfigurationsView);

        assertEquals(emptyMap(), actual);
    }
}
