package com.sequenceiq.cloudbreak.converter;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.controller.json.BlueprintRequest;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;

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
/*
    TODO: test blueprint convert with source url
    @Test
    public void testConvertWithSourceUrl() throws IOException {
        // GIVEN
        String classPackage = getClass().getPackage().getName().replaceAll("\\.", "/");
        Resource resource = new ClassPathResource(classPackage + "/" + "stack/ambari-blueprint.json");
        String url = resource.getURL().toString().replace(":/", ":///");
        ObjectMapper mapper = new ObjectMapper();
        BlueprintRequest request = mapper.readValue(
                "{\"name\":\"multi-node-hdfs-yarn\", \"description\":\"blueprint description\"," +
                        "\"public\":true, \"url\": \"" + url + "\"}"
                , BlueprintRequest.class);
        // WHEN
        Blueprint result = underTest.convert(request);
        // THEN
        assertAllFieldsNotNull(result);
    }
*/
    @Override
    public Class<BlueprintRequest> getRequestClass() {
        return BlueprintRequest.class;
    }
}
