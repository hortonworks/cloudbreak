package com.sequenceiq.cloudbreak.cloud.aws;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.sequenceiq.cloudbreak.cloud.handler.ResourcePersistenceHandler;

@Configuration
@EnableRetry
public class TestApplicationContext {

    @MockBean
    private AwsResourceConnector awsResourceConnector;

    @MockBean
    private CloudFormationTemplateBuilder cloudFormationTemplateBuilder;

    @MockBean
    private ResourcePersistenceHandler resourcePersistenceHandler;

    @MockBean
    private AwsClient awsClient;

    @MockBean
    private AmazonEC2Client ec2Client;

    @Bean
    public AwsInstanceConnector awsInstanceConnector() {
        return new AwsInstanceConnector();
    }
}
