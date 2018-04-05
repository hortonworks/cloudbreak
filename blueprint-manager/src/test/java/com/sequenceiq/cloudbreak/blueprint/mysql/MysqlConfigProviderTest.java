package com.sequenceiq.cloudbreak.blueprint.mysql;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class MysqlConfigProviderTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private BlueprintProcessorFactory blueprintProcessorFactory;

    @Mock
    private BlueprintTextProcessor blueprintProcessor;

    @InjectMocks
    private MysqlConfigProvider underTest;

    @Test
    public void testRemoveComponentFromBlueprintWhenBlueprintContainsMysqlServerThenShouldReturnTrue() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        BlueprintPreparationObject object = BlueprintPreparationObject.Builder.builder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.HIVE)))
                .build();

        when(blueprintProcessor.removeComponentFromBlueprint("MYSQL_SERVER")).thenReturn(blueprintProcessor);
        when(blueprintProcessor.asText()).thenReturn(blueprintText);

        Assert.assertEquals(blueprintText, underTest.customTextManipulation(object, blueprintProcessor).asText());

        verify(blueprintProcessor, times(1)).removeComponentFromBlueprint("MYSQL_SERVER");
    }

    @Test
    public void testAdditionalCriteriaWhenBlueprintContainsMysqlServerThenShouldReturnTrue() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        BlueprintPreparationObject object = BlueprintPreparationObject.Builder.builder()
                .withRdsConfigs(Sets.newHashSet(TestUtil.rdsConfig(RdsType.HIVE)))
                .build();

        when(blueprintProcessor.componentExistsInBlueprint("MYSQL_SERVER")).thenReturn(true);
        when(blueprintProcessorFactory.get(anyString())).thenReturn(blueprintProcessor);

        Assert.assertTrue(underTest.additionalCriteria(object, blueprintText));
    }

    @Test
    public void testAdditionalCriteriaWhenRdsConfigEmptyThenShouldReturnFalse() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        BlueprintPreparationObject object = BlueprintPreparationObject.Builder.builder()
                .build();

        Assert.assertFalse(underTest.additionalCriteria(object, blueprintText));
    }

}