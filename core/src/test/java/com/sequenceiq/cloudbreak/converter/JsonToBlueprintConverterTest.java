package com.sequenceiq.cloudbreak.converter;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintUtils;

public class JsonToBlueprintConverterTest extends AbstractJsonConverterTest<BlueprintRequest> {

    @InjectMocks
    private JsonToBlueprintConverter underTest;

    @Mock
    private JsonHelper jsonHelper;

    @Mock
    private BlueprintUtils blueprintUtils;

    @Before
    public void setUp() {
        underTest = new JsonToBlueprintConverter();
        MockitoAnnotations.initMocks(this);
        when(blueprintUtils.countHostGroups(anyObject())).thenReturn(2);
        when(blueprintUtils.getBlueprintName(anyObject())).thenReturn("bpname");
    }

    @Test
    public void testConvert() throws JsonProcessingException {
        // GIVEN
        // WHEN
        Blueprint result = underTest.convert(getRequest("stack/blueprint.json"));
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("inputParameters"));
    }

    @Override
    public Class<BlueprintRequest> getRequestClass() {
        return BlueprintRequest.class;
    }
}
