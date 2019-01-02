package com.sequenceiq.cloudbreak.converter.v4.stacks.environment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.environment.EnvironmentSettingsV4Response;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentViewToEnvironmentSettingsV4ResponseConverterTest {

    private static final String ENVIRONMENT = "environment";

    private static final String CREDENTIAL = "credential";

    private static final String AVAILABILITY_ZONE = "zone";

    private static final String REGION = "region";

    @InjectMocks
    private EnvironmentViewToEnvironmentSettingsV4ResponseConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Test
    public void testConvertWithEnvironment() {
        EnvironmentView source = mock(EnvironmentView.class);

        Credential credential = mock(Credential.class);

        when(source.getCredential()).thenReturn(credential);
        when(source.getName()).thenReturn(ENVIRONMENT);

        CredentialV4Response credentialV4Response = new CredentialV4Response();
        when(conversionService.convert(credential, CredentialV4Response.class)).thenReturn(credentialV4Response);

        EnvironmentSettingsV4Response result = underTest.convert(source);

        assertNotNull(result);
        assertEquals(ENVIRONMENT, result.getName());
        assertEquals(credentialV4Response, result.getCredential());
    }

}