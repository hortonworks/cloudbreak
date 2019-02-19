package com.sequenceiq.cloudbreak.converter.v4.clusterdefinition;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.IOException;
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
import org.mockito.Spy;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.requests.ClusterDefinitionV4Request;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractJsonConverterTest;
import com.sequenceiq.cloudbreak.clusterdefinition.utils.AmbariBlueprintUtils;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;

public class ClusterDefinitionV4RequestToClusterDefinitionConverterTest extends AbstractJsonConverterTest<ClusterDefinitionV4Request> {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private ClusterDefinitionV4RequestToClusterDefinitionConverter underTest;

    @Mock
    private JsonHelper jsonHelper;

    @Spy
    private final AmbariBlueprintUtils ambariBlueprintUtils = new AmbariBlueprintUtils();

    @Mock
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(2).when(ambariBlueprintUtils).countHostGroups(any());
        doReturn("bpname").when(ambariBlueprintUtils).getBlueprintName(any());
    }

    @Test
    public void testConvertWhereEveryDataFilledButThereIsNoTagsElementInBlueprintJsonThenItShouldBeEmpty() {
        ClusterDefinition result = underTest.convert(getRequest("blueprint.json"));
        assertAllFieldsNotNull(result);
        Assert.assertEquals("{}", result.getTags().getValue());
        Assert.assertEquals("HDP", result.getStackType());
        Assert.assertEquals("2.3", result.getStackVersion());
    }

    @Test
    public void testConvertWhenInputJsonHasTagsFieldButItsEmpty() {
        ClusterDefinition result = underTest.convert(getRequest("blueprint-empty-tags.json"));
        assertAllFieldsNotNull(result);
        Assert.assertEquals("{}", result.getTags().getValue());
        Assert.assertEquals("HDP", result.getStackType());
        Assert.assertEquals("2.3", result.getStackVersion());
    }

    @Test
    public void testConvertWhenInputJsonHasTagsFieldAndItHasMoreThanOneFieldInIt() {
        ClusterDefinition result = underTest.convert(getRequest("blueprint-filled-tags.json"));
        assertAllFieldsNotNull(result);
        Assert.assertTrue(result.getTags().getMap().size() > 1);
        Assert.assertEquals("HDP", result.getStackType());
        Assert.assertEquals("2.3", result.getStackVersion());
    }

    @Test
    public void testConvertWhenUrlIsNotEmptyButInvalidThenExceptionWouldCome() {
        String wrongUrl = "some wrong content for url";
        ClusterDefinitionV4Request request = getRequest("blueprint.json");
        request.setUrl(wrongUrl);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(String.format("Cannot download ambari validation from: %s", wrongUrl));

        underTest.convert(request);
    }

    @Test
    public void testConvertWhenUrlIsNotNullButEmptyThenBlueprintTextShouldBeTheProvidedAmbariBlueprint() {
        ClusterDefinitionV4Request request = getRequest("blueprint.json");
        request.setUrl("");

        ClusterDefinition result = underTest.convert(request);

        Assert.assertEquals(request.getClusterDefinition(), result.getClusterDefinitionText());
        Assert.assertEquals("HDP", result.getStackType());
        Assert.assertEquals("2.3", result.getStackVersion());
    }

    @Test
    public void testConvertWhenNameIsFilledThenTheSameShoulBeInTheBlueprintObject() {
        String name = "name";
        ClusterDefinitionV4Request request = getRequest("blueprint.json");
        request.setName(name);

        ClusterDefinition result = underTest.convert(request);

        Assert.assertEquals(name, result.getName());
        Assert.assertEquals("HDP", result.getStackType());
        Assert.assertEquals("2.3", result.getStackVersion());
    }

    @Test
    public void testConvertWhenNameIsNullThenGeneratedNameShouldBeRepresentedInTheBlueprintObject() {
        String generatedName = "something generated here";
        ClusterDefinitionV4Request request = getRequest("blueprint.json");
        request.setName(null);
        when(missingResourceNameGenerator.generateName(APIResourceType.CLUSTER_DEFINITION)).thenReturn(generatedName);

        ClusterDefinition result = underTest.convert(request);

        Assert.assertEquals(generatedName, result.getName());
        Assert.assertEquals("HDP", result.getStackType());
        Assert.assertEquals("2.3", result.getStackVersion());
    }

    @Test
    public void testConvertWhenUnableToObtainTheBlueprintNameFromTheProvidedBlueprintTextThenExceptionWouldCome() {
        doAnswer(invocation -> {
            throw new IOException("some message");
        }).when(ambariBlueprintUtils).getBlueprintName(any());
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Invalid cluster definition: Failed to parse JSON.");

        underTest.convert(getRequest("blueprint.json"));
    }

    @Test
    public void testConvertWhenUnableToObtainHostGroupCountThenExceptionWouldCome() {
        doAnswer(invocation -> {
            throw new IOException("some message");
        }).when(ambariBlueprintUtils).countHostGroups(any());
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Invalid cluster definition: Failed to parse JSON.");

        underTest.convert(getRequest("blueprint.json"));
    }

    @Test
    public void testConvertWhenUnableToObtainTheStackTypeFromTheProvidedBlueprintTextThenExceptionWouldCome() {
        doAnswer(invocation -> {
            throw new IOException("some message");
        }).when(ambariBlueprintUtils).getBlueprintStackName(any());
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Invalid cluster definition: Failed to parse JSON.");

        underTest.convert(getRequest("blueprint.json"));
    }

    @Test
    public void testConvertWhenUnableToObtainTheStackVersionFromTheProvidedBlueprintTextThenExceptionWouldCome() {
        doAnswer(invocation -> {
            throw new IOException("some message");
        }).when(ambariBlueprintUtils).getBlueprintStackVersion(any());
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Invalid cluster definition: Failed to parse JSON.");

        underTest.convert(getRequest("blueprint.json"));
    }

    @Test
    public void testConvertWhenUnableToCreateJsonFromIncomingTagsThenExceptionWouldCome() {
        ClusterDefinitionV4Request request = getRequest("blueprint.json");
        Map<String, Object> invalidTags = new HashMap<>(1);
        invalidTags.put(null, null);
        request.setTags(invalidTags);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Invalid tag(s) in the cluster definition: Unable to parse JSON.");

        underTest.convert(request);
    }

    @Test(expected = BadRequestException.class)
    public void testWithInvalidDashInHostgroupName() {
        underTest.convert(getRequest("blueprint-hostgroup-name-with-dash.json"));
    }

    @Test
    public void testWithInvalidUnderscoreInHostgroupName() {
        ClusterDefinition result = underTest.convert(getRequest("blueprint-hostgroup-name-with-underscore.json"));
        assertNotNull(result);
    }

    @Override
    public Class<ClusterDefinitionV4Request> getRequestClass() {
        return ClusterDefinitionV4Request.class;
    }

}
