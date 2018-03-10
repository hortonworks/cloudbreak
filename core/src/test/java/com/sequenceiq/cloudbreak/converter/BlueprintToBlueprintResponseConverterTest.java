package com.sequenceiq.cloudbreak.converter;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;

public class BlueprintToBlueprintResponseConverterTest extends AbstractEntityConverterTest<Blueprint> {

    @InjectMocks
    private BlueprintToBlueprintResponseConverter underTest;

    @Mock
    private JsonHelper jsonHelper;

    @Mock
    private JsonNode jsonNode;

    @Before
    public void setUp() {
        underTest = new BlueprintToBlueprintResponseConverter();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() {
        // GIVEN
        given(jsonHelper.createJsonFromString(anyString())).willReturn(jsonNode);
        given(jsonNode.toString()).willReturn("dummyAmbariBlueprint");
        // WHEN
        BlueprintResponse result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Override
    public Blueprint createSource() {
        return TestUtil.blueprint();
    }
}
