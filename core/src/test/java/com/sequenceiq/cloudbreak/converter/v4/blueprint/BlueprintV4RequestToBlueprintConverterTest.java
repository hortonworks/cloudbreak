package com.sequenceiq.cloudbreak.converter.v4.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateGeneratorService;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.converter.ResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractJsonConverterTest;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintHybridOption;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@ExtendWith(MockitoExtension.class)
public class BlueprintV4RequestToBlueprintConverterTest extends AbstractJsonConverterTest<BlueprintV4Request> {

    @InjectMocks
    private BlueprintV4RequestToBlueprintConverter underTest;

    @Mock
    private JsonHelper jsonHelper;

    @Spy
    private final BlueprintUtils blueprintUtils = new BlueprintUtils();

    @Mock
    private ResourceNameGenerator resourceNameGenerator;

    @Mock
    private CmTemplateGeneratorService clusterTemplateGeneratorService;

    @BeforeEach
    public void setUp() {
        lenient().doReturn(2).when(blueprintUtils).countHostGroups(any());
        lenient().doReturn("bpname").when(blueprintUtils).getBlueprintName(any());
    }

    @Test
    public void testConvertShouldThrowExceptionWhenTheBlueprintJsonIsInvalid() {
        BlueprintV4Request request = new BlueprintV4Request();
        String blueprint = "{}";
        request.setBlueprint(blueprint);
        Assertions.assertThrows(BadRequestException.class, () -> underTest.convert(request));
    }

    @Test
    public void testConvertWhenUrlIsNotEmptyButInvalidThenExceptionWouldCome() {
        String wrongUrl = "some wrong content for url";
        BlueprintV4Request request = getRequest("blueprint.json");
        request.setUrl(wrongUrl);

        BadRequestException badRequestException = Assertions.assertThrows(BadRequestException.class, () -> underTest.convert(request));
        assertEquals(String.format("Cannot download cluster template from: %s", wrongUrl), badRequestException.getMessage());
    }

    @Test
    public void testWithInvalidDashInHostgroupName() {
        Assertions.assertThrows(BadRequestException.class, () -> underTest.convert(getRequest("blueprint-hostgroup-name-with-dash.json")));
    }

    @Test
    public void acceptsBuiltinClouderaManagerTemplate() {
        BlueprintV4Request request = new BlueprintV4Request();
        request.setBlueprint(FileReaderUtils.readFileFromClasspathQuietly("defaults/blueprints/7.2.12/cdp-sdx.bp"));
        Blueprint result = underTest.convert(request);
        Assertions.assertNotNull(result);
        assertEquals("CDH", result.getStackType());
        assertEquals("7.2.12", result.getStackVersion());
        assertEquals(2, result.getHostGroupCount());
        Assertions.assertNotNull(result.getBlueprintJsonText());
        Assertions.assertNotEquals("", result.getBlueprintJsonText());
    }

    @Test
    public void rejectsBuiltinWithoutContent() {
        BlueprintV4Request request = new BlueprintV4Request();
        request.setBlueprint("{ \"blueprint\": {}");

        BadRequestException badRequestException = Assertions.assertThrows(BadRequestException.class, () -> underTest.convert(request));
        assertEquals("Invalid cluster template: Failed to parse JSON.", badRequestException.getMessage());
    }

    @Test
    public void rejectsBuiltinWithInvalidContent() {
        BlueprintV4Request request = new BlueprintV4Request();
        request.setBlueprint("{ \"blueprint\": { \"cdhVersion\": \"7.0.0\", { } }");

        BadRequestException badRequestException = Assertions.assertThrows(BadRequestException.class, () -> underTest.convert(request));
        assertEquals("Invalid cluster template: Failed to parse JSON.", badRequestException.getMessage());
    }

    @Override
    public Class<BlueprintV4Request> getRequestClass() {
        return BlueprintV4Request.class;
    }

    @Test
    public void testConvertValidEnums() {
        BlueprintV4Request request = new BlueprintV4Request();
        String blueprint = FileReaderUtils.readFileFromClasspathQuietly("test/defaults/blueprints/blueprint-with-upgrade-option.bp");
        request.setBlueprint(blueprint);
        Blueprint bp = underTest.convert(request);
        assertEquals(BlueprintUpgradeOption.GA, bp.getBlueprintUpgradeOption());
        assertEquals(BlueprintHybridOption.BURST_TO_CLOUD, bp.getHybridOption());
    }

    @Test
    public void testConvertEnumFallbacks() {
        BlueprintV4Request request = new BlueprintV4Request();
        String blueprint = FileReaderUtils.readFileFromClasspathQuietly("test/defaults/blueprints/blueprint-with-repositories.bp");
        request.setBlueprint(blueprint);
        Blueprint bp = underTest.convert(request);
        assertEquals(BlueprintUpgradeOption.ENABLED, bp.getBlueprintUpgradeOption());
        assertEquals(BlueprintHybridOption.NONE, bp.getHybridOption());
    }
}