package com.sequenceiq.cloudbreak.converter;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.blueprint.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;

public class BlueprintRequestToBlueprintConverterTest extends AbstractJsonConverterTest<BlueprintRequest> {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private BlueprintRequestToBlueprintConverter underTest;

    @Mock
    private JsonHelper jsonHelper;

    @Mock
    private BlueprintUtils blueprintUtils;

    @Mock
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(blueprintUtils.countHostGroups(anyObject())).thenReturn(2);
        when(blueprintUtils.getBlueprintName(anyObject())).thenReturn("bpname");
    }

    @Test
    public void testConvertWhereEveryDataFilledButThereIsNoTagsElementInBlueprintJsonThenItShouldBeEmpty() {
        Blueprint result = underTest.convert(getRequest("stack/blueprint.json"));
        assertAllFieldsNotNull(result, Collections.singletonList("inputParameters"));
        Assert.assertEquals("{}", result.getTags().getValue());
    }

    @Test
    public void testConvertWhenInputJsonHasTagsFieldButItsEmpty() {
        Blueprint result = underTest.convert(getRequest("stack/blueprint-empty-tags.json"));
        assertAllFieldsNotNull(result, Collections.singletonList("inputParameters"));
        Assert.assertEquals("{}", result.getTags().getValue());
    }

    @Test
    public void testConvertWhenInputJsonHasTagsFieldAndItHasMoreThanOneFieldInIt() {
        Blueprint result = underTest.convert(getRequest("stack/blueprint-filled-tags.json"));
        assertAllFieldsNotNull(result, Collections.singletonList("inputParameters"));
        Assert.assertTrue(result.getTags().getMap().size() > 1);
    }

    @Test(expected = BadRequestException.class)
    public void testWithNonAlphaNumericHostGroupName() {
        underTest.convert(getRequest("stack/blueprint-non-alpha-numeric-host-group-name.json"));
    }

    @Test
    public void testConvertWhenUrlIsNotEmptyButInvalidThenExceptionWouldCome() throws IOException {
        String wrongUrl = "some wrong content for url";
        BlueprintRequest request = getRequest("stack/blueprint.json");
        request.setUrl(wrongUrl);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(String.format("Cannot download ambari validation from: %s", wrongUrl));

        underTest.convert(request);
    }

    @Test
    public void testConvertWhenUrlIsNotNullButEmptyThenBlueprintTextShouldBeTheProvidedAmbariBlueprint() {
        BlueprintRequest request = getRequest("stack/blueprint.json");
        request.setUrl("");

        Blueprint result = underTest.convert(request);

        Assert.assertEquals(request.getAmbariBlueprint(), result.getBlueprintText());
    }

    @Test
    public void testConvertWhenNameIsFilledThenTheSameShoulBeInTheBlueprintObject() {
        String name = "name";
        BlueprintRequest request = getRequest("stack/blueprint.json");
        request.setName(name);

        Blueprint result = underTest.convert(request);

        Assert.assertEquals(name, result.getName());
    }

    @Test
    public void testConvertWhenNameIsNullThenGeneratedNameShouldBeRepresentedInTheBlueprintObject() {
        String generatedName = "something generated here";
        BlueprintRequest request = getRequest("stack/blueprint.json");
        request.setName(null);
        when(missingResourceNameGenerator.generateName(APIResourceType.BLUEPRINT)).thenReturn(generatedName);

        Blueprint result = underTest.convert(request);

        Assert.assertEquals(generatedName, result.getName());
    }

    @Test
    public void testConvertWhenUnableToObtainTheBlueprintNameFromTheProvidedBlueprintTextThenExceptionWouldCome() {
        when(blueprintUtils.getBlueprintName(any())).thenAnswer(invocation -> {
            throw new IOException("some message");
        });

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Invalid Blueprint: Failed to parse JSON.");

        underTest.convert(getRequest("stack/blueprint.json"));
    }

    @Test
    public void testConvertWhenUnableToObtainHostGroupCountThenExceptionWouldCome() {
        when(blueprintUtils.countHostGroups(any())).thenAnswer(invocation -> {
            throw new IOException("some message");
        });

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Invalid Blueprint: Failed to parse JSON.");

        underTest.convert(getRequest("stack/blueprint.json"));
    }

    @Test
    public void testConvertWhenUnableToCreateJsonFromIncomingTagsThenExceptionWouldCome() {
        BlueprintRequest request = getRequest("stack/blueprint.json");
        Map<String, Object> invalidTags = new HashMap<>(1);
        invalidTags.put(null, null);
        request.setTags(invalidTags);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Invalid tag(s) in the Blueprint: Unable to parse JSON.");

        underTest.convert(request);
    }

    @Override
    public Class<BlueprintRequest> getRequestClass() {
        return BlueprintRequest.class;
    }

}
