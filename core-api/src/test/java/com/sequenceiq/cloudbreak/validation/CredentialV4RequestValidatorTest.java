package com.sequenceiq.cloudbreak.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.AwsCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AzureCredentialV4RequestParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.cumulus.CumulusYarnCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.GcpCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.mock.MockCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.yarn.YarnCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;

public class CredentialV4RequestValidatorTest {

    private CredentialV4RequestValidator underTest;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Before
    public void setUp() {
        underTest = new CredentialV4RequestValidator();
        MockitoAnnotations.initMocks(this);
        ConstraintViolationBuilder constraintViolationBuilder = mock(ConstraintViolationBuilder.class);
        NodeBuilderCustomizableContext nodeBuilderCustomizableContext = mock(NodeBuilderCustomizableContext.class);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(constraintViolationBuilder);
        when(constraintViolationBuilder.addPropertyNode(any())).thenReturn(nodeBuilderCustomizableContext);
    }

    @Test
    public void testWhenOnlyAwsParametersHasSetThenTheRequestIsValid() {
        CredentialV4Request request = new CredentialV4Request();
        request.setAws(new AwsCredentialV4Parameters());

        assertTrue(underTest.isValid(request, constraintValidatorContext));
    }

    @Test
    public void testWhenOnlyAzureParametersHasSetThenTheRequestIsValid() {
        CredentialV4Request request = new CredentialV4Request();
        request.setAzure(new AzureCredentialV4RequestParameters());

        assertTrue(underTest.isValid(request, constraintValidatorContext));
    }

    @Test
    public void testWhenOnlyCumulusYarnParametersHasSetThenTheRequestIsValid() {
        CredentialV4Request request = new CredentialV4Request();
        request.setCumulus(new CumulusYarnCredentialV4Parameters());

        assertTrue(underTest.isValid(request, constraintValidatorContext));
    }

    @Test
    public void testWhenOnlyGcpParametersHasSetThenTheRequestIsValid() {
        CredentialV4Request request = new CredentialV4Request();
        request.setGcp(new GcpCredentialV4Parameters());

        assertTrue(underTest.isValid(request, constraintValidatorContext));
    }

    @Test
    public void testWhenOnlyYarnParametersHasSetThenTheRequestIsValid() {
        CredentialV4Request request = new CredentialV4Request();
        request.setYarn(new YarnCredentialV4Parameters());

        assertTrue(underTest.isValid(request, constraintValidatorContext));
    }

    @Test
    public void testWhenOnlyMockParametersHasSetThenTheRequestIsValid() {
        CredentialV4Request request = new CredentialV4Request();
        request.setMock(new MockCredentialV4Parameters());

        assertTrue(underTest.isValid(request, constraintValidatorContext));
    }

    @Test
    public void testWhenMoreThanOneCredentialParametersHasSetThenTheRequestIsInvalid() {
        CredentialV4Request request = new CredentialV4Request();
        request.setAzure(new AzureCredentialV4RequestParameters());
        request.setGcp(new GcpCredentialV4Parameters());

        assertFalse(underTest.isValid(request, constraintValidatorContext));
    }

}