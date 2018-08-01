package com.sequenceiq.cloudbreak.converter.v2.template;

import static com.sequenceiq.cloudbreak.api.model.v2.template.BaseTemplateParameter.PLATFORM_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.v2.template.YarnParameters;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;

@RunWith(MockitoJUnitRunner.class)
public class YarnTemplateParametersToParametersConverterTest {

    @InjectMocks
    private YarnTemplateParametersToParametersConverter underTest;

    @Test
    public void convert() {
        Map<String, Object> convert = underTest.convert(new YarnParameters());
        assertNotNull(convert);
        assertEquals(CloudConstants.YARN, convert.get(PLATFORM_TYPE));
    }
}