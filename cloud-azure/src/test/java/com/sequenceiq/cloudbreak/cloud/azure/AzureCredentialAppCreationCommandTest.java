package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.AzureCredentialAppCreationCommand.CB_AZ_APP_REDIRECT_URI_PATTERN;
import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

public class AzureCredentialAppCreationCommandTest {

    private static final String GENERATE_EXCEPTION_MESSAGE_FORMAT = "Failed to process the Azure AD App creation template from path: '%s'";

    private static final String APP_CREATION_COMMAND_TEMPLATE_PATH = "somePathForCommandTemplate";

    private static final String APP_AUDIT_CREATION_COMMAND_TEMPLATE_PATH = "someAuditPathForCommandTemplate";

    private static final String MALFORMED_TEMPLATE_NAMED_EXCEPTION_DESCRIPTION = "description";

    private static final String APP_CREATION_JSON_TEMPLATE_PATH = "somePathForJsonTemplate";

    private static final String TEMPLATE_NOT_FOUND_EXCEPTION_MESSAGE = "message";

    private static final String DEPLOYMENT_ADDRESS = "https://192.168.99.100";

    private static final String TEMPLATE_NAME = "some template";

    private static final String ENCODING = "UTF-8";

    private static final String WORKSPACE_ID = "1";

    private static final String MANAGEMENT_API_RESOURCE_APP_ID = "797f4846-ba00-4fd7-ba43-dac1f8f63013";

    private static final String MANAGEMENT_API_RESOURCE_ACCESS_SCOPE_ID = "41094075-9dad-400e-a0bd-54e686782033";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private Configuration freemarkerConfiguration;

    @Mock
    private Template template;

    @InjectMocks
    private AzureCredentialAppCreationCommand underTest;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(underTest, "appCreationCommandTemplatePath", APP_CREATION_COMMAND_TEMPLATE_PATH);
        ReflectionTestUtils.setField(underTest, "appAuditCreationCommandTemplatePath", APP_AUDIT_CREATION_COMMAND_TEMPLATE_PATH);
        ReflectionTestUtils.setField(underTest, "appCreationJSONTemplatePath", APP_CREATION_JSON_TEMPLATE_PATH);
        ReflectionTestUtils.setField(underTest, "resourceAppId", MANAGEMENT_API_RESOURCE_APP_ID);
        ReflectionTestUtils.setField(underTest, "resourceAccessScopeId", MANAGEMENT_API_RESOURCE_ACCESS_SCOPE_ID);
        when(freemarkerConfiguration.getTemplate(APP_CREATION_COMMAND_TEMPLATE_PATH, ENCODING)).thenReturn(template);
        when(freemarkerConfiguration.getTemplate(APP_AUDIT_CREATION_COMMAND_TEMPLATE_PATH, ENCODING)).thenReturn(template);
        when(freemarkerConfiguration.getTemplate(APP_CREATION_JSON_TEMPLATE_PATH, ENCODING)).thenReturn(template);
    }

    @Test
    public void testEnvironmentWhenFreemarkerConfigGetTemplateThrowsTemplateNotFoundExceptionThenCloudConnectorExceptionComesForAppCreationCommandTemplate()
            throws IOException, TemplateException {
        doThrow(new TemplateNotFoundException(TEMPLATE_NAME, new Object(), TEMPLATE_NOT_FOUND_EXCEPTION_MESSAGE)).when(freemarkerConfiguration)
                .getTemplate(APP_CREATION_COMMAND_TEMPLATE_PATH, ENCODING);

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage(format(GENERATE_EXCEPTION_MESSAGE_FORMAT, APP_CREATION_COMMAND_TEMPLATE_PATH));

        underTest.generateEnvironmentCredentialCommand(DEPLOYMENT_ADDRESS);

        verify(template, times(0)).process(any(), any(StringWriter.class));
        verify(freemarkerConfiguration, times(1)).getTemplate(anyString(), anyString());
        verify(freemarkerConfiguration, times(1)).getTemplate(APP_CREATION_COMMAND_TEMPLATE_PATH, ENCODING);
    }

    @Test
    public void testEnvironmentWhenFreemarkerConfigGetTemplateThrowsMalformedTemplateNameExceptionThenCloudConnectorExceptionComesForAppCreationCommandTemplate()
            throws IOException, TemplateException {
        doThrow(new MalformedTemplateNameException(TEMPLATE_NAME, MALFORMED_TEMPLATE_NAMED_EXCEPTION_DESCRIPTION)).when(freemarkerConfiguration)
                .getTemplate(APP_CREATION_COMMAND_TEMPLATE_PATH, ENCODING);

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage(format(GENERATE_EXCEPTION_MESSAGE_FORMAT, APP_CREATION_COMMAND_TEMPLATE_PATH));

        underTest.generateEnvironmentCredentialCommand(DEPLOYMENT_ADDRESS);

        verify(template, times(0)).process(any(), any(StringWriter.class));
        verify(freemarkerConfiguration, times(1)).getTemplate(anyString(), anyString());
        verify(freemarkerConfiguration, times(1)).getTemplate(APP_CREATION_COMMAND_TEMPLATE_PATH, ENCODING);
    }

    @Test
    public void testEnvironmentWhenFreemarkerConfigGetTemplateThrowsIOExceptionThenCloudConnectorExceptionComesForAppCreationCommandTemplate()
            throws IOException, TemplateException {
        doThrow(new IOException()).when(freemarkerConfiguration).getTemplate(APP_CREATION_COMMAND_TEMPLATE_PATH, ENCODING);

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage(format(GENERATE_EXCEPTION_MESSAGE_FORMAT, APP_CREATION_COMMAND_TEMPLATE_PATH));

        underTest.generateEnvironmentCredentialCommand(DEPLOYMENT_ADDRESS);

        verify(template, times(0)).process(any(), any(StringWriter.class));
        verify(freemarkerConfiguration, times(1)).getTemplate(anyString(), anyString());
        verify(freemarkerConfiguration, times(1)).getTemplate(APP_CREATION_COMMAND_TEMPLATE_PATH, ENCODING);
    }

    @Test
    public void testEnvironmentWhenTemplateProcessThrowsTemplateExceptionThenCloudConnectorExceptionComesForAppCreationCommandTemplate()
            throws IOException, TemplateException {
        doThrow(new TemplateException(Environment.getCurrentEnvironment())).when(template).process(any(), any(StringWriter.class));

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage(format(GENERATE_EXCEPTION_MESSAGE_FORMAT, APP_CREATION_COMMAND_TEMPLATE_PATH));

        underTest.generateEnvironmentCredentialCommand(DEPLOYMENT_ADDRESS);

        verify(template, times(1)).process(any(), any(StringWriter.class));
        verify(freemarkerConfiguration, times(1)).getTemplate(anyString(), anyString());
        verify(freemarkerConfiguration, times(1)).getTemplate(APP_CREATION_COMMAND_TEMPLATE_PATH, ENCODING);
    }

    @Test
    public void testEnvironmentWhenTemplateProcessThrowsIOExceptionThenCloudConnectorExceptionComesForAppCreationCommandTemplate()
            throws IOException, TemplateException {
        doThrow(new IOException()).when(template).process(any(), any(StringWriter.class));

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage(format(GENERATE_EXCEPTION_MESSAGE_FORMAT, APP_CREATION_COMMAND_TEMPLATE_PATH));

        underTest.generateEnvironmentCredentialCommand(DEPLOYMENT_ADDRESS);

        verify(template, times(1)).process(any(), any(StringWriter.class));
        verify(freemarkerConfiguration, times(1)).getTemplate(anyString(), anyString());
        verify(freemarkerConfiguration, times(1)).getTemplate(APP_CREATION_COMMAND_TEMPLATE_PATH, ENCODING);
    }

    @Test
    public void testEnvironmentWhenNoExceptionComesFromExecutionThenEverythingShouldGoFineForAppCreationCommandTemplate() throws IOException, TemplateException {
        doNothing().when(template).process(any(), any(StringWriter.class));

        underTest.generateEnvironmentCredentialCommand(DEPLOYMENT_ADDRESS);

        verify(template, times(1)).process(any(), any(StringWriter.class));
        verify(freemarkerConfiguration, times(1)).getTemplate(anyString(), anyString());
        verify(freemarkerConfiguration, times(1)).getTemplate(APP_CREATION_COMMAND_TEMPLATE_PATH, ENCODING);
    }

    @Test
    public void testEnvironmentWhenDeploymentAddressIsEmptyThenEverythingShouldGoFineForAppCreationCommandTemplateWithDefaultDeployment()
            throws IOException, TemplateException {
        doNothing().when(template).process(any(), any(StringWriter.class));

        underTest.generateEnvironmentCredentialCommand("");

        verify(template, times(1)).process(any(), any(StringWriter.class));
        verify(freemarkerConfiguration, times(1)).getTemplate(anyString(), anyString());
        verify(freemarkerConfiguration, times(1)).getTemplate(APP_CREATION_COMMAND_TEMPLATE_PATH, ENCODING);
    }

    @Test
    public void testAuditWhenFreemarkerConfigGetTemplateThrowsTemplateNotFoundExceptionThenCloudConnectorExceptionComesForAppCreationCommandTemplate()
            throws IOException, TemplateException {
        doThrow(new TemplateNotFoundException(TEMPLATE_NAME, new Object(), TEMPLATE_NOT_FOUND_EXCEPTION_MESSAGE)).when(freemarkerConfiguration)
                .getTemplate(APP_AUDIT_CREATION_COMMAND_TEMPLATE_PATH, ENCODING);

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage(format(GENERATE_EXCEPTION_MESSAGE_FORMAT, APP_AUDIT_CREATION_COMMAND_TEMPLATE_PATH));

        underTest.generateAuditCredentialCommand(DEPLOYMENT_ADDRESS);

        verify(template, times(0)).process(any(), any(StringWriter.class));
        verify(freemarkerConfiguration, times(1)).getTemplate(anyString(), anyString());
        verify(freemarkerConfiguration, times(1)).getTemplate(APP_AUDIT_CREATION_COMMAND_TEMPLATE_PATH, ENCODING);
    }

    @Test
    public void testAuditWhenFreemarkerConfigGetTemplateThrowsMalformedTemplateNameExceptionThenCloudConnectorExceptionComesForAppCreationCommandTemplate()
            throws IOException, TemplateException {
        doThrow(new MalformedTemplateNameException(TEMPLATE_NAME, MALFORMED_TEMPLATE_NAMED_EXCEPTION_DESCRIPTION)).when(freemarkerConfiguration)
                .getTemplate(APP_AUDIT_CREATION_COMMAND_TEMPLATE_PATH, ENCODING);

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage(format(GENERATE_EXCEPTION_MESSAGE_FORMAT, APP_AUDIT_CREATION_COMMAND_TEMPLATE_PATH));

        underTest.generateAuditCredentialCommand(DEPLOYMENT_ADDRESS);

        verify(template, times(0)).process(any(), any(StringWriter.class));
        verify(freemarkerConfiguration, times(1)).getTemplate(anyString(), anyString());
        verify(freemarkerConfiguration, times(1)).getTemplate(APP_AUDIT_CREATION_COMMAND_TEMPLATE_PATH, ENCODING);
    }

    @Test
    public void testAuditWhenFreemarkerConfigGetTemplateThrowsIOExceptionThenCloudConnectorExceptionComesForAppCreationCommandTemplate()
            throws IOException, TemplateException {
        doThrow(new IOException()).when(freemarkerConfiguration).getTemplate(APP_AUDIT_CREATION_COMMAND_TEMPLATE_PATH, ENCODING);

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage(format(GENERATE_EXCEPTION_MESSAGE_FORMAT, APP_AUDIT_CREATION_COMMAND_TEMPLATE_PATH));

        underTest.generateAuditCredentialCommand(DEPLOYMENT_ADDRESS);

        verify(template, times(0)).process(any(), any(StringWriter.class));
        verify(freemarkerConfiguration, times(1)).getTemplate(anyString(), anyString());
        verify(freemarkerConfiguration, times(1)).getTemplate(APP_AUDIT_CREATION_COMMAND_TEMPLATE_PATH, ENCODING);
    }

    @Test
    public void testAuditWhenTemplateProcessThrowsTemplateExceptionThenCloudConnectorExceptionComesForAppCreationCommandTemplate()
            throws IOException, TemplateException {
        doThrow(new TemplateException(Environment.getCurrentEnvironment())).when(template).process(any(), any(StringWriter.class));

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage(format(GENERATE_EXCEPTION_MESSAGE_FORMAT, APP_AUDIT_CREATION_COMMAND_TEMPLATE_PATH));

        underTest.generateAuditCredentialCommand(DEPLOYMENT_ADDRESS);

        verify(template, times(1)).process(any(), any(StringWriter.class));
        verify(freemarkerConfiguration, times(1)).getTemplate(anyString(), anyString());
        verify(freemarkerConfiguration, times(1)).getTemplate(APP_AUDIT_CREATION_COMMAND_TEMPLATE_PATH, ENCODING);
    }

    @Test
    public void testAuditWhenTemplateProcessThrowsIOExceptionThenCloudConnectorExceptionComesForAppCreationCommandTemplate()
            throws IOException, TemplateException {
        doThrow(new IOException()).when(template).process(any(), any(StringWriter.class));

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage(format(GENERATE_EXCEPTION_MESSAGE_FORMAT, APP_AUDIT_CREATION_COMMAND_TEMPLATE_PATH));

        underTest.generateAuditCredentialCommand(DEPLOYMENT_ADDRESS);

        verify(template, times(1)).process(any(), any(StringWriter.class));
        verify(freemarkerConfiguration, times(1)).getTemplate(anyString(), anyString());
        verify(freemarkerConfiguration, times(1)).getTemplate(APP_AUDIT_CREATION_COMMAND_TEMPLATE_PATH, ENCODING);
    }

    @Test
    public void testAuditWhenNoExceptionComesFromExecutionThenEverythingShouldGoFineForAppCreationCommandTemplate() throws IOException, TemplateException {
        doNothing().when(template).process(any(), any(StringWriter.class));

        underTest.generateAuditCredentialCommand(DEPLOYMENT_ADDRESS);

        verify(template, times(1)).process(any(), any(StringWriter.class));
        verify(freemarkerConfiguration, times(1)).getTemplate(anyString(), anyString());
        verify(freemarkerConfiguration, times(1)).getTemplate(APP_AUDIT_CREATION_COMMAND_TEMPLATE_PATH, ENCODING);
    }

    @Test
    public void testAuditWhenDeploymentAddressIsEmptyThenEverythingShouldGoFineForAppCreationCommandTemplateWithDefaultDeployment()
            throws IOException, TemplateException {
        doNothing().when(template).process(any(), any(StringWriter.class));

        underTest.generateAuditCredentialCommand("");

        verify(template, times(1)).process(any(), any(StringWriter.class));
        verify(freemarkerConfiguration, times(1)).getTemplate(anyString(), anyString());
        verify(freemarkerConfiguration, times(1)).getTemplate(APP_AUDIT_CREATION_COMMAND_TEMPLATE_PATH, ENCODING);
    }

    @Test
    public void testGenerateWhenFreemarkerConfigGetTemplateThrowsTemplateNotFoundExceptionThenCloudConnectorExceptionComesForAppCreationJsonTemplate()
            throws IOException, TemplateException {
        doThrow(new TemplateNotFoundException(TEMPLATE_NAME, new Object(), TEMPLATE_NOT_FOUND_EXCEPTION_MESSAGE)).when(freemarkerConfiguration)
                .getTemplate(APP_CREATION_JSON_TEMPLATE_PATH, ENCODING);

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage(format(GENERATE_EXCEPTION_MESSAGE_FORMAT, APP_CREATION_JSON_TEMPLATE_PATH));

        underTest.generateJSON(DEPLOYMENT_ADDRESS);

        verify(template, times(0)).process(any(), any(StringWriter.class));
        verify(freemarkerConfiguration, times(1)).getTemplate(anyString(), anyString());
        verify(freemarkerConfiguration, times(1)).getTemplate(APP_CREATION_JSON_TEMPLATE_PATH, ENCODING);
    }

    @Test
    public void testGenerateWhenFreemarkerConfigGetTemplateThrowsMalformedTemplateNameExceptionThenCloudConnectorExceptionComesForAppCreationJsonTemplate()
            throws IOException, TemplateException {
        doThrow(new MalformedTemplateNameException(TEMPLATE_NAME, MALFORMED_TEMPLATE_NAMED_EXCEPTION_DESCRIPTION)).when(freemarkerConfiguration)
                .getTemplate(APP_CREATION_JSON_TEMPLATE_PATH, ENCODING);

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage(format(GENERATE_EXCEPTION_MESSAGE_FORMAT, APP_CREATION_JSON_TEMPLATE_PATH));

        underTest.generateJSON(DEPLOYMENT_ADDRESS);

        verify(template, times(0)).process(any(), any(StringWriter.class));
        verify(freemarkerConfiguration, times(1)).getTemplate(anyString(), anyString());
        verify(freemarkerConfiguration, times(1)).getTemplate(APP_CREATION_JSON_TEMPLATE_PATH, ENCODING);
    }

    @Test
    public void testGenerateWhenFreemarkerConfigGetTemplateThrowsIOExceptionThenCloudConnectorExceptionComesForAppCreationJsonTemplate()
            throws IOException, TemplateException {
        doThrow(new IOException()).when(freemarkerConfiguration).getTemplate(APP_CREATION_JSON_TEMPLATE_PATH, ENCODING);

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage(format(GENERATE_EXCEPTION_MESSAGE_FORMAT, APP_CREATION_JSON_TEMPLATE_PATH));

        underTest.generateJSON(DEPLOYMENT_ADDRESS);

        verify(template, times(0)).process(any(), any(StringWriter.class));
        verify(freemarkerConfiguration, times(1)).getTemplate(anyString(), anyString());
        verify(freemarkerConfiguration, times(1)).getTemplate(APP_CREATION_JSON_TEMPLATE_PATH, ENCODING);
    }

    @Test
    public void testGenerateWhenTemplateProcessThrowsTemplateExceptionThenCloudConnectorExceptionComesForAppCreationJsonTemplate()
            throws IOException, TemplateException {
        doThrow(new TemplateException(Environment.getCurrentEnvironment())).when(template).process(any(), any(StringWriter.class));

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage(format(GENERATE_EXCEPTION_MESSAGE_FORMAT, APP_CREATION_JSON_TEMPLATE_PATH));

        underTest.generateJSON(DEPLOYMENT_ADDRESS);

        verify(template, times(1)).process(any(), any(StringWriter.class));
        verify(freemarkerConfiguration, times(1)).getTemplate(anyString(), anyString());
        verify(freemarkerConfiguration, times(1)).getTemplate(APP_CREATION_JSON_TEMPLATE_PATH, ENCODING);
    }

    @Test
    public void testGenerateWhenTemplateProcessThrowsIOExceptionThenCloudConnectorExceptionComesForAppCreationJsonTemplate()
            throws IOException, TemplateException {
        doThrow(new IOException()).when(template).process(any(), any(StringWriter.class));

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage(format(GENERATE_EXCEPTION_MESSAGE_FORMAT, APP_CREATION_JSON_TEMPLATE_PATH));

        underTest.generateJSON(DEPLOYMENT_ADDRESS);

        verify(template, times(1)).process(any(), any(StringWriter.class));
        verify(freemarkerConfiguration, times(1)).getTemplate(anyString(), anyString());
        verify(freemarkerConfiguration, times(1)).getTemplate(APP_CREATION_JSON_TEMPLATE_PATH, ENCODING);
    }

    @Test
    public void testWhenNoExceptionComesFromExecutionThenEverythingShouldGoFineForAppCreationJsonTemplate() throws IOException, TemplateException {
        doNothing().when(template).process(any(), any(StringWriter.class));

        underTest.generateJSON(DEPLOYMENT_ADDRESS);

        verify(template, times(1)).process(any(), any(StringWriter.class));
        verify(freemarkerConfiguration, times(1)).getTemplate(anyString(), anyString());
        verify(freemarkerConfiguration, times(1)).getTemplate(APP_CREATION_JSON_TEMPLATE_PATH, ENCODING);
    }

    @Test
    public void testGetRedirectURLWhenDeploymentAddressDoesNotEndsWithExpectedDelimiterThenItShouldHaveInsertedBetweenDeploymentAddressAndAuthUri() {
        String deploymentAddressWithoutDelimiterAtEnd = "https://192.168.99.100";
        String expected = format("%s/%s", deploymentAddressWithoutDelimiterAtEnd, format(CB_AZ_APP_REDIRECT_URI_PATTERN, WORKSPACE_ID));

        String result = underTest.getRedirectURL(deploymentAddressWithoutDelimiterAtEnd);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetRedirectURLWhenDeploymentAddressEndsWithExpectedDelimiterThenDeploymentAddressAndAuthUriShouldComeBackConcatenatedAfterEachOther() {
        String deploymentAddressWithDelimiterAtEnd = "https://192.168.99.100/";
        String expected = format("%s%s", deploymentAddressWithDelimiterAtEnd, format(CB_AZ_APP_REDIRECT_URI_PATTERN, WORKSPACE_ID));

        String result = underTest.getRedirectURL(deploymentAddressWithDelimiterAtEnd);

        Assert.assertEquals(expected, result);
    }

}
