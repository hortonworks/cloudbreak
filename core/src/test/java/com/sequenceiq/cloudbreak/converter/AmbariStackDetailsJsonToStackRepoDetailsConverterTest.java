package com.sequenceiq.cloudbreak.converter;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;

public class AmbariStackDetailsJsonToStackRepoDetailsConverterTest extends AbstractJsonConverterTest<AmbariStackDetailsJson> {

    private AmbariStackDetailsJsonToStackRepoDetailsConverter underTest;

    @Before
    public void setUp() {
        underTest = new AmbariStackDetailsJsonToStackRepoDetailsConverter();
    }

    @Test
    public void testConvertWhenBaseImage() {
        StackRepoDetails result = underTest.convert(getRequest("stack/ambari-stack-details-base-image.json"));

        assertAllFieldsNotNull(result, Collections.singletonList("knox"));

        Assert.assertFalse(result.getStack().containsKey(StackRepoDetails.CUSTOM_VDF_REPO_KEY));
    }

    @Test
    public void testConvertWhenVDFProvided() {

        AmbariStackDetailsJson request = getRequest("stack/ambari-stack-details-vdf.json");
        StackRepoDetails result = underTest.convert(request);

        Assert.assertFalse(result.getStack().containsKey(request.getOs()));
    }

    @Override
    public Class<AmbariStackDetailsJson> getRequestClass() {
        return AmbariStackDetailsJson.class;
    }
}
