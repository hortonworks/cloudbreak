package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.AwsCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;
import com.sequenceiq.cloudbreak.controller.validation.credential.CredentialValidator;
import com.sequenceiq.cloudbreak.converter.v4.credentials.CredentialV4RequestToCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.credential.CredentialPropertyCollector;

@RunWith(Parameterized.class)
public class CredentialV4RequestToCredentialConverterTest {

    private Object govCloud;

    private Boolean expectedGovCloudFlag;

    @Mock
    private CredentialPropertyCollector credentialPropertyCollector;

    @Mock
    private CredentialValidator credentialValidator;

    @InjectMocks
    private CredentialV4RequestToCredentialConverter underTest;

    public CredentialV4RequestToCredentialConverterTest(Object govCloud, Boolean expectedGovCloudFlag) {
        this.govCloud = govCloud;
        this.expectedGovCloudFlag = expectedGovCloudFlag;
    }

    @Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> params = new ArrayList<>();
        params.add(new Object[]{"true", Boolean.TRUE});
        params.add(new Object[]{Boolean.TRUE, Boolean.TRUE});
        params.add(new Object[]{"false", Boolean.FALSE});
        params.add(new Object[]{Boolean.FALSE, Boolean.FALSE});
        params.add(new Object[]{null, Boolean.FALSE});
        return params;
    }

    @Before
    public void setup() {
        initMocks(this);
        when(credentialPropertyCollector.propertyMap(any())).thenReturn(Maps.newHashMap());
    }

    @Test
    public void testGovCloudFlagConversion() {
        CredentialV4Request request = new CredentialV4Request();
        request.setAws(createParametersWithGovCloud(govCloud));

        Credential credential = underTest.convert(request);

        assertEquals(expectedGovCloudFlag, credential.getGovCloud());
    }

    private AwsCredentialV4Parameters createParametersWithGovCloud(Object govCloud) {
        AwsCredentialV4Parameters awsCredentialV4Parameters = new AwsCredentialV4Parameters();
        if (govCloud != null) {
            if (govCloud instanceof String) {
                awsCredentialV4Parameters.setGovCloud(Boolean.valueOf((String) govCloud));
            } else {
                awsCredentialV4Parameters.setGovCloud((Boolean) govCloud);
            }
        }
        return awsCredentialV4Parameters;
    }
}
