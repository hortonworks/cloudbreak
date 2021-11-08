package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.aws.TestConstants.LATEST_AWS_CLOUD_FORMATION_DB_TEMPLATE_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder.RDSModelContext;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

@ExtendWith(MockitoExtension.class)
class CloudFormationTemplateBuilderDBTest {

    private static final String CIDR_1 = "10.0.0.0/16";

    private static final String CIDR_2 = "10.1.0.0/16";

    @Mock
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @InjectMocks
    private CloudFormationTemplateBuilder cloudFormationTemplateBuilder;

    private FreeMarkerConfigurationFactoryBean factoryBean;

    static Iterable<?> templatesPathDataProvider() {
        List<String> templates = Lists.newArrayList(LATEST_AWS_CLOUD_FORMATION_DB_TEMPLATE_PATH);
        File[] templateFiles = new File(CloudFormationTemplateBuilderDBTest.class.getClassLoader().getResource("dbtemplates").getPath()).listFiles();
        List<String> olderTemplates = Arrays.stream(templateFiles).map(file -> {
            String[] path = file.getPath().split("/");
            return "templates/" + path[path.length - 1];
        }).filter(s -> !s.contains(".keep")).collect(Collectors.toList());
        templates.addAll(olderTemplates);
        return templates;
    }

    @BeforeEach
    void setUp() throws Exception {
        factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        ReflectionTestUtils.setField(cloudFormationTemplateBuilder, "freemarkerConfiguration", factoryBean.getObject());

        when(freeMarkerTemplateUtils.processTemplateIntoString(any(), any())).thenCallRealMethod();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("templatesPathDataProvider")
    void buildTestWhenHavingSecurityGroupAndNoPortAndNoSslEnforcementAndNoSslCertificateIdentifier(String templatePath) throws IOException {
        //GIVEN
        String awsCloudFormationTemplate = factoryBean.getObject().getTemplate(templatePath, "UTF-8").toString();
        //WHEN
        RDSModelContext modelContext = new RDSModelContext()
                .withHasSecurityGroup(true)
                .withUseSslEnforcement(false)
                .withSslCertificateIdentifierDefined(false)
                .withTemplate(awsCloudFormationTemplate);
        String result = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(JsonUtil.isValid(result)).overridingErrorMessage("Invalid JSON: " + result).isTrue();
        assertThat(result).doesNotContain("\"PortParameter\": {");
        assertThat(result).doesNotContain("\"DBParameterGroupNameParameter\": {");
        assertThat(result).doesNotContain("\"DBParameterGroupFamilyParameter\": {");
        assertThat(result).doesNotContain("\"SslCertificateIdentifierParameter\": {");
        assertThat(result).contains("\"VPCSecurityGroupsParameter\": {");
        assertThat(result).doesNotContain("\"DBSecurityGroupNameParameter\": {");
        assertThat(result).doesNotContain("\"VPCIdParameter\": {");
        assertThat(result).doesNotContain("\"VPCSecurityGroup\": {");
        assertThat(result).doesNotContain("\"FromPort\"");
        assertThat(result).doesNotContain("\"ToPort\"");
        assertThat(result).doesNotContain("\"CidrIp\" :");
        assertThat(result).doesNotContain("\"DBParameterGroup\": {");
        assertThat(result).doesNotContain("\"DBParameterGroupName\": { \"Ref\": \"DBParameterGroup\" },");
        assertThat(result).doesNotContain("\"CACertificateIdentifier\": { \"Ref\": \"SslCertificateIdentifierParameter\" },");
        assertThat(result).doesNotContain("\"Port\": { \"Ref\": \"PortParameter\" },");
        assertThat(result).contains("\"VPCSecurityGroups\": { \"Ref\": \"VPCSecurityGroupsParameter\" }");
        assertThat(result).doesNotContain("\"VPCSecurityGroups\": [{ \"Ref\": \"VPCSecurityGroup\" }]");
        assertThat(result).doesNotContain("\"CreatedDBParameterGroup\": { \"Value\": { \"Ref\": \"DBParameterGroup\" } },");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("templatesPathDataProvider")
    void buildTestWhenHavingSecurityGroupAndNoPortAndNoSslEnforcementAndWithSslCertificateIdentifier(String templatePath) throws IOException {
        //GIVEN
        String awsCloudFormationTemplate = factoryBean.getObject().getTemplate(templatePath, "UTF-8").toString();
        //WHEN
        RDSModelContext modelContext = new RDSModelContext()
                .withHasSecurityGroup(true)
                .withUseSslEnforcement(false)
                .withSslCertificateIdentifierDefined(true)
                .withTemplate(awsCloudFormationTemplate);
        String result = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(JsonUtil.isValid(result)).overridingErrorMessage("Invalid JSON: " + result).isTrue();
        assertThat(result).doesNotContain("\"PortParameter\": {");
        assertThat(result).doesNotContain("\"DBParameterGroupNameParameter\": {");
        assertThat(result).doesNotContain("\"DBParameterGroupFamilyParameter\": {");
        assertThat(result).doesNotContain("\"SslCertificateIdentifierParameter\": {");
        assertThat(result).contains("\"VPCSecurityGroupsParameter\": {");
        assertThat(result).doesNotContain("\"DBSecurityGroupNameParameter\": {");
        assertThat(result).doesNotContain("\"VPCIdParameter\": {");
        assertThat(result).doesNotContain("\"VPCSecurityGroup\": {");
        assertThat(result).doesNotContain("\"FromPort\"");
        assertThat(result).doesNotContain("\"ToPort\"");
        assertThat(result).doesNotContain("\"CidrIp\" :");
        assertThat(result).doesNotContain("\"DBParameterGroup\": {");
        assertThat(result).doesNotContain("\"DBParameterGroupName\": { \"Ref\": \"DBParameterGroup\" },");
        assertThat(result).doesNotContain("\"CACertificateIdentifier\": { \"Ref\": \"SslCertificateIdentifierParameter\" },");
        assertThat(result).doesNotContain("\"Port\": { \"Ref\": \"PortParameter\" },");
        assertThat(result).contains("\"VPCSecurityGroups\": { \"Ref\": \"VPCSecurityGroupsParameter\" }");
        assertThat(result).doesNotContain("\"VPCSecurityGroups\": [{ \"Ref\": \"VPCSecurityGroup\" }]");
        assertThat(result).doesNotContain("\"CreatedDBParameterGroup\": { \"Value\": { \"Ref\": \"DBParameterGroup\" } },");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("templatesPathDataProvider")
    void buildTestWhenHavingSecurityGroupAndNoPortAndUsingSslEnforcementAndNoSslCertificateIdentifier(String templatePath) throws IOException {
        //GIVEN
        String awsCloudFormationTemplate = factoryBean.getObject().getTemplate(templatePath, "UTF-8").toString();
        //WHEN
        RDSModelContext modelContext = new RDSModelContext()
                .withHasSecurityGroup(true)
                .withUseSslEnforcement(true)
                .withSslCertificateIdentifierDefined(false)
                .withTemplate(awsCloudFormationTemplate);
        String result = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(JsonUtil.isValid(result)).overridingErrorMessage("Invalid JSON: " + result).isTrue();
        assertThat(result).doesNotContain("\"PortParameter\": {");
        assertThat(result).contains("\"DBParameterGroupNameParameter\": {");
        assertThat(result).contains("\"DBParameterGroupFamilyParameter\": {");
        assertThat(result).doesNotContain("\"SslCertificateIdentifierParameter\": {");
        assertThat(result).contains("\"VPCSecurityGroupsParameter\": {");
        assertThat(result).doesNotContain("\"DBSecurityGroupNameParameter\": {");
        assertThat(result).doesNotContain("\"VPCIdParameter\": {");
        assertThat(result).doesNotContain("\"VPCSecurityGroup\": {");
        assertThat(result).doesNotContain("\"FromPort\"");
        assertThat(result).doesNotContain("\"ToPort\"");
        assertThat(result).doesNotContain("\"CidrIp\" :");
        assertThat(result).contains("\"DBParameterGroup\": {");
        assertThat(result).contains("\"Parameters\": { \"rds.force_ssl\": \"1\" },");
        assertThat(result).contains("\"DBParameterGroupName\": { \"Ref\": \"DBParameterGroup\" },");
        assertThat(result).doesNotContain("\"CACertificateIdentifier\": { \"Ref\": \"SslCertificateIdentifierParameter\" },");
        assertThat(result).doesNotContain("\"Port\": { \"Ref\": \"PortParameter\" },");
        assertThat(result).contains("\"VPCSecurityGroups\": { \"Ref\": \"VPCSecurityGroupsParameter\" }");
        assertThat(result).doesNotContain("\"VPCSecurityGroups\": [{ \"Ref\": \"VPCSecurityGroup\" }]");
        assertThat(result).contains("\"CreatedDBParameterGroup\": { \"Value\": { \"Ref\": \"DBParameterGroup\" } },");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("templatesPathDataProvider")
    void buildTestWhenHavingSecurityGroupAndNoPortAndUsingSslEnforcementAndWithSslCertificateIdentifier(String templatePath) throws IOException {
        //GIVEN
        String awsCloudFormationTemplate = factoryBean.getObject().getTemplate(templatePath, "UTF-8").toString();
        //WHEN
        RDSModelContext modelContext = new RDSModelContext()
                .withHasSecurityGroup(true)
                .withUseSslEnforcement(true)
                .withSslCertificateIdentifierDefined(true)
                .withTemplate(awsCloudFormationTemplate);
        String result = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(JsonUtil.isValid(result)).overridingErrorMessage("Invalid JSON: " + result).isTrue();
        assertThat(result).doesNotContain("\"PortParameter\": {");
        assertThat(result).contains("\"DBParameterGroupNameParameter\": {");
        assertThat(result).contains("\"DBParameterGroupFamilyParameter\": {");
        assertThat(result).contains("\"SslCertificateIdentifierParameter\": {");
        assertThat(result).contains("\"VPCSecurityGroupsParameter\": {");
        assertThat(result).doesNotContain("\"DBSecurityGroupNameParameter\": {");
        assertThat(result).doesNotContain("\"VPCIdParameter\": {");
        assertThat(result).doesNotContain("\"VPCSecurityGroup\": {");
        assertThat(result).doesNotContain("\"FromPort\"");
        assertThat(result).doesNotContain("\"ToPort\"");
        assertThat(result).doesNotContain("\"CidrIp\" :");
        assertThat(result).contains("\"DBParameterGroup\": {");
        assertThat(result).contains("\"Parameters\": { \"rds.force_ssl\": \"1\" },");
        assertThat(result).contains("\"DBParameterGroupName\": { \"Ref\": \"DBParameterGroup\" },");
        assertThat(result).contains("\"CACertificateIdentifier\": { \"Ref\": \"SslCertificateIdentifierParameter\" },");
        assertThat(result).doesNotContain("\"Port\": { \"Ref\": \"PortParameter\" },");
        assertThat(result).contains("\"VPCSecurityGroups\": { \"Ref\": \"VPCSecurityGroupsParameter\" }");
        assertThat(result).doesNotContain("\"VPCSecurityGroups\": [{ \"Ref\": \"VPCSecurityGroup\" }]");
        assertThat(result).contains("\"CreatedDBParameterGroup\": { \"Value\": { \"Ref\": \"DBParameterGroup\" } },");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("templatesPathDataProvider")
    void buildTestWhenHavingSecurityGroupAndHavingPort(String templatePath) throws IOException {
        //GIVEN
        String awsCloudFormationTemplate = factoryBean.getObject().getTemplate(templatePath, "UTF-8").toString();
        //WHEN
        RDSModelContext modelContext = new RDSModelContext()
                .withHasSecurityGroup(true)
                .withHasPort(true)
                .withTemplate(awsCloudFormationTemplate);
        String result = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(JsonUtil.isValid(result)).overridingErrorMessage("Invalid JSON: " + result).isTrue();
        assertThat(result).contains("\"PortParameter\": {");
        assertThat(result).doesNotContain("\"DBParameterGroupNameParameter\": {");
        assertThat(result).doesNotContain("\"DBParameterGroupFamilyParameter\": {");
        assertThat(result).contains("\"VPCSecurityGroupsParameter\": {");
        assertThat(result).doesNotContain("\"DBSecurityGroupNameParameter\": {");
        assertThat(result).doesNotContain("\"VPCIdParameter\": {");
        assertThat(result).doesNotContain("\"VPCSecurityGroup\": {");
        assertThat(result).doesNotContain("\"FromPort\"");
        assertThat(result).doesNotContain("\"ToPort\"");
        assertThat(result).doesNotContain("\"CidrIp\" :");
        assertThat(result).doesNotContain("\"DBParameterGroup\": {");
        assertThat(result).doesNotContain("\"DBParameterGroupName\": { \"Ref\": \"DBParameterGroup\" },");
        assertThat(result).contains("\"Port\": { \"Ref\": \"PortParameter\" },");
        assertThat(result).contains("\"VPCSecurityGroups\": { \"Ref\": \"VPCSecurityGroupsParameter\" }");
        assertThat(result).doesNotContain("\"VPCSecurityGroups\": [{ \"Ref\": \"VPCSecurityGroup\" }]");
        assertThat(result).doesNotContain("\"CreatedDBParameterGroup\": { \"Value\": { \"Ref\": \"DBParameterGroup\" } },");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("templatesPathDataProvider")
    void buildTestWhenNoSecurityGroupAndNoPortAndSingleCidr(String templatePath) throws IOException {
        //GIVEN
        String awsCloudFormationTemplate = factoryBean.getObject().getTemplate(templatePath, "UTF-8").toString();
        //WHEN
        RDSModelContext modelContext = new RDSModelContext()
                .withNetworkCidrs(List.of(CIDR_1))
                .withTemplate(awsCloudFormationTemplate);
        String result = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(JsonUtil.isValid(result)).overridingErrorMessage("Invalid JSON: " + result).isTrue();
        assertThat(result).doesNotContain("\"PortParameter\": {");
        assertThat(result).doesNotContain("\"DBParameterGroupNameParameter\": {");
        assertThat(result).doesNotContain("\"DBParameterGroupFamilyParameter\": {");
        assertThat(result).doesNotContain("\"VPCSecurityGroupsParameter\": {");
        assertThat(result).contains("\"DBSecurityGroupNameParameter\": {");
        assertThat(result).contains("\"VPCIdParameter\": {");
        assertThat(result).contains("\"VPCSecurityGroup\": {");
        assertThat(result).contains("\"FromPort\": 5432,");
        assertThat(result).contains("\"ToPort\" : 5432,");
        assertThat(result).doesNotContain("\"FromPort\": { \"Ref\": \"PortParameter\" },");
        assertThat(result).doesNotContain("\"ToPort\" : { \"Ref\": \"PortParameter\" },");
        assertThat(result).contains(String.format("\"CidrIp\" : \"%s\"", CIDR_1));
        assertThat(result).doesNotContain("\"DBParameterGroup\": {");
        assertThat(result).doesNotContain("\"DBParameterGroupName\": { \"Ref\": \"DBParameterGroup\" },");
        assertThat(result).doesNotContain("\"Port\": { \"Ref\": \"PortParameter\" },");
        assertThat(result).doesNotContain("\"VPCSecurityGroups\": { \"Ref\": \"VPCSecurityGroupsParameter\" }");
        assertThat(result).contains("\"VPCSecurityGroups\": [{ \"Ref\": \"VPCSecurityGroup\" }]");
        assertThat(result).doesNotContain("\"CreatedDBParameterGroup\": { \"Value\": { \"Ref\": \"DBParameterGroup\" } },");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("templatesPathDataProvider")
    void buildTestWhenNoSecurityGroupAndNoPortAndMultipleCidr(String templatePath) throws IOException {
        //GIVEN
        String awsCloudFormationTemplate = factoryBean.getObject().getTemplate(templatePath, "UTF-8").toString();
        //WHEN
        RDSModelContext modelContext = new RDSModelContext()
                .withNetworkCidrs(List.of(CIDR_1, CIDR_2))
                .withTemplate(awsCloudFormationTemplate);
        String result = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(JsonUtil.isValid(result)).overridingErrorMessage("Invalid JSON: " + result).isTrue();
        assertThat(result).doesNotContain("\"PortParameter\": {");
        assertThat(result).doesNotContain("\"DBParameterGroupNameParameter\": {");
        assertThat(result).doesNotContain("\"DBParameterGroupFamilyParameter\": {");
        assertThat(result).doesNotContain("\"VPCSecurityGroupsParameter\": {");
        assertThat(result).contains("\"DBSecurityGroupNameParameter\": {");
        assertThat(result).contains("\"VPCIdParameter\": {");
        assertThat(result).contains("\"VPCSecurityGroup\": {");
        assertThat(result).contains("\"FromPort\": 5432,");
        assertThat(result).contains("\"ToPort\" : 5432,");
        assertThat(result).doesNotContain("\"FromPort\": { \"Ref\": \"PortParameter\" },");
        assertThat(result).doesNotContain("\"ToPort\" : { \"Ref\": \"PortParameter\" },");
        assertThat(result).contains(String.format("\"CidrIp\" : \"%s\"", CIDR_1));
        assertThat(result).contains(String.format("\"CidrIp\" : \"%s\"", CIDR_2));
        assertThat(result).doesNotContain("\"DBParameterGroup\": {");
        assertThat(result).doesNotContain("\"DBParameterGroupName\": { \"Ref\": \"DBParameterGroup\" },");
        assertThat(result).doesNotContain("\"Port\": { \"Ref\": \"PortParameter\" },");
        assertThat(result).doesNotContain("\"VPCSecurityGroups\": { \"Ref\": \"VPCSecurityGroupsParameter\" }");
        assertThat(result).contains("\"VPCSecurityGroups\": [{ \"Ref\": \"VPCSecurityGroup\" }]");
        assertThat(result).doesNotContain("\"CreatedDBParameterGroup\": { \"Value\": { \"Ref\": \"DBParameterGroup\" } },");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("templatesPathDataProvider")
    void buildTestWhenNoSecurityGroupAndHavingPortAndMultipleCidr(String templatePath) throws IOException {
        //GIVEN
        String awsCloudFormationTemplate = factoryBean.getObject().getTemplate(templatePath, "UTF-8").toString();
        //WHEN
        RDSModelContext modelContext = new RDSModelContext()
                .withHasPort(true)
                .withNetworkCidrs(List.of(CIDR_1, CIDR_2))
                .withTemplate(awsCloudFormationTemplate);
        String result = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        assertThat(JsonUtil.isValid(result)).overridingErrorMessage("Invalid JSON: " + result).isTrue();
        assertThat(result).contains("\"PortParameter\": {");
        assertThat(result).doesNotContain("\"DBParameterGroupNameParameter\": {");
        assertThat(result).doesNotContain("\"DBParameterGroupFamilyParameter\": {");
        assertThat(result).doesNotContain("\"VPCSecurityGroupsParameter\": {");
        assertThat(result).contains("\"DBSecurityGroupNameParameter\": {");
        assertThat(result).contains("\"VPCIdParameter\": {");
        assertThat(result).contains("\"VPCSecurityGroup\": {");
        assertThat(result).doesNotContain("\"FromPort\": 5432,");
        assertThat(result).doesNotContain("\"ToPort\" : 5432,");
        assertThat(result).contains("\"FromPort\": { \"Ref\": \"PortParameter\" },");
        assertThat(result).contains("\"ToPort\" : { \"Ref\": \"PortParameter\" },");
        assertThat(result).contains(String.format("\"CidrIp\" : \"%s\"", CIDR_1));
        assertThat(result).contains(String.format("\"CidrIp\" : \"%s\"", CIDR_2));
        assertThat(result).doesNotContain("\"DBParameterGroup\": {");
        assertThat(result).doesNotContain("\"DBParameterGroupName\": { \"Ref\": \"DBParameterGroup\" },");
        assertThat(result).contains("\"Port\": { \"Ref\": \"PortParameter\" },");
        assertThat(result).doesNotContain("\"VPCSecurityGroups\": { \"Ref\": \"VPCSecurityGroupsParameter\" }");
        assertThat(result).contains("\"VPCSecurityGroups\": [{ \"Ref\": \"VPCSecurityGroup\" }]");
        assertThat(result).doesNotContain("\"CreatedDBParameterGroup\": { \"Value\": { \"Ref\": \"DBParameterGroup\" } },");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("templatesPathDataProvider")
    void buildTestWhenKmsKeyIdPresent(String templatePath) throws IOException {
        //GIVEN
        String awsCloudFormationTemplate = factoryBean.getObject().getTemplate(templatePath, "UTF-8").toString();
        //WHEN
        RDSModelContext modelContext = new RDSModelContext()
                .withIsKmsCustom(true)
                .withGetKmsKey("dummyKeyArn")
                .withTemplate(awsCloudFormationTemplate);
        String result = cloudFormationTemplateBuilder.build(modelContext);
        assertThat(JsonUtil.isValid(result)).overridingErrorMessage("Invalid JSON: " + result).isTrue();
        assertThat(result).contains("\"StorageEncrypted\": true");
        assertThat(result).contains("\"KmsKeyId\" : \"dummyKeyArn\"");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("templatesPathDataProvider")
    void buildTestWhenKmsKeyIdAbsent(String templatePath) throws IOException {
        //GIVEN
        String awsCloudFormationTemplate = factoryBean.getObject().getTemplate(templatePath, "UTF-8").toString();
        //WHEN
        RDSModelContext modelContext = new RDSModelContext()
                .withIsKmsCustom(false)
                .withTemplate(awsCloudFormationTemplate);
        String result = cloudFormationTemplateBuilder.build(modelContext);
        assertThat(JsonUtil.isValid(result)).overridingErrorMessage("Invalid JSON: " + result).isTrue();
        assertThat(result).contains("\"StorageEncrypted\": true");
        assertThat(result).doesNotContain("\"KmsKeyId\" : \"dummyKeyArn\"");
    }

}
