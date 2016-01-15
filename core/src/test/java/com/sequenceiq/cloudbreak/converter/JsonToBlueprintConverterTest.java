package com.sequenceiq.cloudbreak.converter;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;

public class JsonToBlueprintConverterTest extends AbstractJsonConverterTest<BlueprintRequest> {

    @InjectMocks
    private JsonToBlueprintConverter underTest;

    @Mock
    private JsonHelper jsonHelper;

    @Before
    public void setUp() {
        underTest = new JsonToBlueprintConverter();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        Blueprint result = underTest.convert(getRequest("stack/blueprint.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Override
    public Class<BlueprintRequest> getRequestClass() {
        return BlueprintRequest.class;
    }
}
