package com.sequenceiq.cloudbreak.converter.v4.stacks.environment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;

@RunWith(MockitoJUnitRunner.class)
public class StackToEnvironmentSettingsV4ResponseConverterTest {

    private static final String ENVIRONMENT = "environment";

    private static final String CREDENTIAL = "credential";

    private static final String AVAILABILITY_ZONE = "zone";

    private static final String REGION = "region";

    @InjectMocks
    private StackToEnvironmentSettingsV4ResponseConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Test
    public void testConvertWithNoEnvironment() {
        Stack source = mock(Stack.class);

        EnvironmentSettingsV4Response result = underTest.convert(source);

        assertNull(result);
    }

    @Test
    public void testConvertWithEnvironment() {
        Stack source = mock(Stack.class);
        when(source.getAvailabilityZone()).thenReturn(AVAILABILITY_ZONE);
        when(source.getRegion()).thenReturn(REGION);

        Credential credential = mock(Credential.class);

        EnvironmentView environment = mock(EnvironmentView.class);
        when(source.getEnvironment()).thenReturn(environment);
        when(environment.getCredential()).thenReturn(credential);
        when(environment.getName()).thenReturn(ENVIRONMENT);

        CredentialV4Response credentialV4Response = new CredentialV4Response();
        when(conversionService.convert(credential, CredentialV4Response.class)).thenReturn(credentialV4Response);

        EnvironmentSettingsV4Response result = underTest.convert(source);

        assertNotNull(result);
        assertEquals(ENVIRONMENT, result.getName());
        assertEquals(credentialV4Response, result.getCredential());
        assertNotNull(result.getPlacement());
        assertEquals(AVAILABILITY_ZONE, result.getPlacement().getAvailabilityZone());
        assertEquals(REGION, result.getPlacement().getRegion());
    }

}