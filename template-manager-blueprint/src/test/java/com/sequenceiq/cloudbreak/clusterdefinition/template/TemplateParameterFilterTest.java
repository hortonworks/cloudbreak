package com.sequenceiq.cloudbreak.clusterdefinition.template;

import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.template.HandleBarModelKey;
import com.sequenceiq.cloudbreak.template.TemplateParameterFilter;

@RunWith(MockitoJUnitRunner.class)
public class TemplateParameterFilterTest {

    @InjectMocks
    private final TemplateParameterFilter underTest = new TemplateParameterFilter();

    @Test
    public void testTemplateFilterWhenWeShouldReturnWithTheRelatedProperties() {
        int inputSize = 20;
        List<String> testData = generateTestData(HandleBarModelKey.DATALAKE.modelKey(), inputSize);
        List<String> resultData = generateResultData(inputSize);

        Set<String> result = underTest.queryForDatalakeParameters(HandleBarModelKey.DATALAKE, testData);

        Assert.assertEquals(inputSize, result.size());
        Assert.assertThat(resultData, containsInAnyOrder(result.toArray()));
    }

    @Test
    public void testTemplateFilterWhenWeShouldReturnWithTheCustomProperties() {
        int inputSize = 20;
        List<String> testData = generateTestData("hadoop", inputSize);

        List<String> hadoop = generateTestData("hadoop", inputSize, new ArrayList<>());

        Set<String> result = underTest.queryForCustomParameters(testData);

        Assert.assertEquals(inputSize, result.size());
        Assert.assertThat(hadoop, containsInAnyOrder(result.toArray()));
    }

    private List<String> generateTestData(String handleBarModelKey, int inputSize) {
        List<String> testData = new ArrayList<>();
        generateTestData(handleBarModelKey, inputSize, testData);
        generateTestData(HandleBarModelKey.CLUSTER_DEFINITION.modelKey(), inputSize, testData);
        generateTestData(HandleBarModelKey.COMPONENTS.modelKey(), inputSize, testData);
        return testData;

    }

    private List<String> generateResultData(int inputSize) {
        List<String> testData = new ArrayList<>();
        generateResultData(inputSize, testData);
        return testData;

    }

    private List<String> generateTestData(String handleBarModelKey, int inputSize, List<String> testData) {
        for (int i = 0; i < inputSize; i++) {
            testData.add(String.format("%s.test%s.test2.test3", handleBarModelKey, i));
        }
        return testData;
    }

    private void generateResultData(int inputSize, List<String> testData) {
        for (int i = 0; i < inputSize; i++) {
            testData.add(String.format("test%s.test2.test3", i));
        }
    }

}