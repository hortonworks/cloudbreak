package com.sequenceiq.cloudbreak.converter;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ambari.StackRepositoryV4RequestToStackRepoDetailsConverter;

public class AmbariStackDetailsJsonToStackRepoDetailsConverterTest extends AbstractJsonConverterTest<StackRepositoryV4Request> {

    private StackRepositoryV4RequestToStackRepoDetailsConverter underTest;

    @Before
    public void setUp() {
        underTest = new StackRepositoryV4RequestToStackRepoDetailsConverter();
    }

    @Test
    public void testConvertWhenBaseImage() {
        StackRepoDetails result = underTest.convert(getRequest("stack/ambari-stack-details-base-image.json"));

        assertAllFieldsNotNull(result, Collections.singletonList("knox"));

        Assert.assertFalse(result.getStack().containsKey(StackRepoDetails.CUSTOM_VDF_REPO_KEY));
    }

    @Test
    public void testConvertWhenVDFProvided() {

        StackRepositoryV4Request request = getRequest("stack/ambari-stack-details-vdf.json");
        StackRepoDetails result = underTest.convert(request);

        Assert.assertFalse(result.getStack().containsKey(request.getOs()));
    }

    @Override
    public Class<StackRepositoryV4Request> getRequestClass() {
        return StackRepositoryV4Request.class;
    }
}
