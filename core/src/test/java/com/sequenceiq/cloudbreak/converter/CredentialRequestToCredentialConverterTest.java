package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.controller.validation.credential.CredentialValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.stack.resource.definition.credential.CredentialDefinitionService;

@RunWith(Parameterized.class)
public class CredentialRequestToCredentialConverterTest {

    private Object govCloud;

    private Boolean expectedGovCloudFlag;

    @Mock
    private CredentialDefinitionService credentialDefinitionService;

    @Mock
    private CredentialValidator credentialValidator;

    @InjectMocks
    private CredentialRequestToCredentialConverter underTest;

    public CredentialRequestToCredentialConverterTest(Object govCloud, Boolean expectedGovCloudFlag) {
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
        doNothing().when(credentialValidator).validateCredentialCloudPlatform(any());
    }

    @Test
    public void testGovCloudFlagConversion() {
        CredentialRequest request = new CredentialRequest();
        request.setParameters(createParametersWithGovCloud(govCloud));

        Credential credential = underTest.convert(request);

        assertEquals(expectedGovCloudFlag, credential.getGovCloud());
    }

    private Map<String, Object> createParametersWithGovCloud(Object govCloud) {
        Map<String, Object> parameters = Maps.newHashMap();
        if (govCloud != null) {
            parameters.put("govCloud", govCloud);
        }
        return parameters;
    }
}
