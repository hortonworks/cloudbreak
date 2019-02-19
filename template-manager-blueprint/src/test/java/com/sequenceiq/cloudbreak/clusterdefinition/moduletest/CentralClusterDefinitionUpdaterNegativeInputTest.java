package com.sequenceiq.cloudbreak.clusterdefinition.moduletest;

import static com.sequenceiq.cloudbreak.clusterdefinition.moduletest.AmbariBlueprintModulTestModelProvider.getPreparedBuilder;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sequenceiq.cloudbreak.clusterdefinition.filesystem.ClusterDefinitionTestUtil;
import com.sequenceiq.cloudbreak.template.ClusterDefinitionProcessingException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
public class CentralClusterDefinitionUpdaterNegativeInputTest extends CentralBlueprintContext {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testForNotExistingHostGroupNameComesFromModel() throws IOException {
        String dummyHostGroupName = "dummy";
        expectedException.expect(ClusterDefinitionProcessingException.class);
        expectedException.expectMessage(String.format("There is no such host group as \"%s\"", dummyHostGroupName));
        TemplatePreparationObject model = getExtendedPreparedBlueprintBuilder("wrong-hostnames-in-model.bp", dummyHostGroupName).build();
        getUnderTest().getClusterDefinitionText(model);
    }

    @Test
    public void testForMissingHostGroupNodeInBlueprint() throws IOException {
        expectedException.expect(ClusterDefinitionProcessingException.class);
        TemplatePreparationObject model = getExtendedPreparedBlueprintBuilder("missing-hostgroups.bp").build();
        getUnderTest().getClusterDefinitionText(model);
    }

    @Test
    public void testForMissingBlueprintsBlockInBlueprint() throws IOException {
        expectedException.expect(ClusterDefinitionProcessingException.class);
        TemplatePreparationObject model = getExtendedPreparedBlueprintBuilder("missing-blueprints-block.bp").build();
        getUnderTest().getClusterDefinitionText(model);
    }

    @Test
    public void testForEmptyJsonInputAsBlueprintText() throws IOException {
        expectedException.expect(ClusterDefinitionProcessingException.class);
        TemplatePreparationObject model = getExtendedPreparedBlueprintBuilder("empty-blueprint.bp").build();
        getUnderTest().getClusterDefinitionText(model);
    }

    @Test
    public void testInvalidJsonInput() throws IOException {
        expectedException.expect(ClusterDefinitionProcessingException.class);
        TemplatePreparationObject model = getExtendedPreparedBlueprintBuilder("invalid-json.bp").build();
        getUnderTest().getClusterDefinitionText(model);
    }

    private Builder getExtendedPreparedBlueprintBuilder(String fileName, String... hosts) throws IOException {
        return getPreparedBuilder(hosts)
                .withClusterDefinitionView(ClusterDefinitionTestUtil.generalBlueprintView(getBlueprintTextFromFile(fileName), "2.6", "HDP"));
    }

    private String getBlueprintTextFromFile(String fileName) throws IOException {
        return FileReaderUtils.readFileFromClasspath(String.format("module-test/invalid-input/%s", fileName));
    }

}
