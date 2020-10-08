package com.sequenceiq.cloudbreak.converter.v4.blueprint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateGeneratorService;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.converter.AbstractJsonConverterTest;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.json.CloudbreakApiException;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public class BlueprintV4RequestToBlueprintConverterTest extends AbstractJsonConverterTest<BlueprintV4Request> {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private BlueprintV4RequestToBlueprintConverter underTest;

    @Mock
    private JsonHelper jsonHelper;

    @Spy
    private final BlueprintUtils blueprintUtils = new BlueprintUtils();

    @Mock
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Mock
    private CmTemplateGeneratorService clusterTemplateGeneratorService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(2).when(blueprintUtils).countHostGroups(any());
        doReturn("bpname").when(blueprintUtils).getBlueprintName(any());
    }

    @Test(expected = BadRequestException.class)
    public void testConvertShouldThrowExceptionWhenTheBlueprintJsonIsInvalid() {
        BlueprintV4Request request = new BlueprintV4Request();
        String blueprint = "{}";
        request.setBlueprint(blueprint);
        when(jsonHelper.createJsonFromString(blueprint)).thenThrow(new CloudbreakApiException("Invalid Json"));
        underTest.convert(request);
    }

    @Test
    public void testConvertWhenUrlIsNotEmptyButInvalidThenExceptionWouldCome() {
        String wrongUrl = "some wrong content for url";
        BlueprintV4Request request = getRequest("blueprint.json");
        request.setUrl(wrongUrl);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(String.format("Cannot download cluster template from: %s", wrongUrl));

        underTest.convert(request);
    }

    @Test(expected = BadRequestException.class)
    public void testWithInvalidDashInHostgroupName() {
        underTest.convert(getRequest("blueprint-hostgroup-name-with-dash.json"));
    }

    @Test
    public void acceptsBuiltinClouderaManagerTemplate() {
        BlueprintV4Request request = new BlueprintV4Request();
        request.setBlueprint(FileReaderUtils.readFileFromClasspathQuietly("defaults/blueprints/cdp-sdx-702.bp"));
        Blueprint result = underTest.convert(request);
        assertNotNull(result);
        assertEquals("CDH", result.getStackType());
        assertEquals("7.0.2", result.getStackVersion());
        assertEquals(2, result.getHostGroupCount());
        assertNotNull(result.getBlueprintText());
        assertNotEquals("", result.getBlueprintText());
    }

    @Test
    public void rejectsBuiltinWithoutContent() {
        BlueprintV4Request request = new BlueprintV4Request();
        request.setBlueprint("{ \"blueprint\": {}");
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Invalid cluster template: Failed to parse JSON.");

        underTest.convert(request);
    }

    @Test
    public void rejectsBuiltinWithInvalidContent() {
        BlueprintV4Request request = new BlueprintV4Request();
        request.setBlueprint("{ \"blueprint\": { \"cdhVersion\": \"7.0.0\", { } }");
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Invalid cluster template: Failed to parse JSON.");

        underTest.convert(request);
    }

    @Override
    public Class<BlueprintV4Request> getRequestClass() {
        return BlueprintV4Request.class;
    }

}
