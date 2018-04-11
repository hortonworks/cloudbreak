package com.sequenceiq.cloudbreak.blueprint.moduletest;

import com.sequenceiq.cloudbreak.blueprint.filesystem.BlueprintTestUtil;
import com.sequenceiq.cloudbreak.templateprocessor.processor.PreparationObject;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateProcessingException;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

import static com.sequenceiq.cloudbreak.blueprint.moduletest.BlueprintModelProvider.getPreparedBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
public class CentralBlueprintUpdaterNegativeInputTest extends CentralBlueprintContext {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testForNotExistingHostGroupNameComesFromModel() throws IOException {
        String dummyHostGroupName = "dummy";
        expectedException.expect(TemplateProcessingException.class);
        expectedException.expectMessage(String.format("There is no such host group as \"%s\"", dummyHostGroupName));
        PreparationObject model = getExtendedPreparedBlueprintBuilder("wrong-hostnames-in-model.bp", dummyHostGroupName).build();
        getUnderTest().getBlueprintText(model);
    }

    @Test
    public void testForMissingHostGroupNodeInBlueprint() throws IOException {
        expectedException.expect(TemplateProcessingException.class);
        PreparationObject model = getExtendedPreparedBlueprintBuilder("missing-hostgroups.bp").build();
        getUnderTest().getBlueprintText(model);
    }

    @Test
    public void testForMissingBlueprintsBlockInBlueprint() throws IOException {
        expectedException.expect(TemplateProcessingException.class);
        PreparationObject model = getExtendedPreparedBlueprintBuilder("missing-blueprints-block.bp").build();
        getUnderTest().getBlueprintText(model);
    }

    @Test
    public void testForEmptyJsonInputAsBlueprintText() throws IOException {
        expectedException.expect(TemplateProcessingException.class);
        PreparationObject model = getExtendedPreparedBlueprintBuilder("empty-blueprint.bp").build();
        getUnderTest().getBlueprintText(model);
    }

    @Test
    public void testInvalidJsonInput() throws IOException {
        expectedException.expect(TemplateProcessingException.class);
        PreparationObject model = getExtendedPreparedBlueprintBuilder("invalid-json.bp").build();
        getUnderTest().getBlueprintText(model);
    }

    private PreparationObject.Builder getExtendedPreparedBlueprintBuilder(String fileName, String... hosts) throws IOException {
        return getPreparedBuilder(hosts)
                .withBlueprintView(BlueprintTestUtil.generalBlueprintView(getBlueprintTextFromFile(fileName), "2.6", "HDP"));
    }

    private String getBlueprintTextFromFile(String fileName) throws IOException {
        return FileReaderUtils.readFileFromClasspath(String.format("module-test/invalid-input/%s", fileName));
    }

}
