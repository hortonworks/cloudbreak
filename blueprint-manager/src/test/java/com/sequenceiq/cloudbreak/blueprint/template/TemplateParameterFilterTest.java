package com.sequenceiq.cloudbreak.blueprint.template;

import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TemplateParameterFilterTest {

    @InjectMocks
    private final TemplateParameterFilter underTest = new TemplateParameterFilter();

    @Test
    public void testTemplateFilterWhenWeShouldResturnWithTheRelatedProperties() {
        int inputSize = 20;
        List<String> testData = generateTestData(HandleBarModelKey.DATALAKE, inputSize);
        List<String> resultData = generateResultData(inputSize);

        Set<String> result = underTest.queryForDatalakeParameters(HandleBarModelKey.DATALAKE, testData);

        Assert.assertEquals(inputSize, result.size());
        Assert.assertThat(resultData, containsInAnyOrder(result.toArray()));
    }

    private List<String> generateTestData(HandleBarModelKey handleBarModelKey, int inputSize) {
        List<String> testData = new ArrayList<>();
        generateTestData(handleBarModelKey, inputSize, testData);
        generateTestData(HandleBarModelKey.BLUEPRINT, inputSize, testData);
        generateTestData(HandleBarModelKey.COMPONENTS, inputSize, testData);
        return testData;

    }

    private List<String> generateResultData(int inputSize) {
        List<String> testData = new ArrayList<>();
        generateResultData(inputSize, testData);
        return testData;

    }

    private void generateTestData(HandleBarModelKey handleBarModelKey, int inputSize, List<String> testData) {
        for (int i = 0; i < inputSize; i++) {
            testData.add(String.format("%s.test%s.test2.test3", handleBarModelKey.modelKey(), i));
        }
    }

    private void generateResultData(int inputSize, List<String> testData) {
        for (int i = 0; i < inputSize; i++) {
            testData.add(String.format("test%s.test2.test3", i));
        }
    }

}