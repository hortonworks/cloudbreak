package com.sequenceiq.cloudbreak.cloud.aws;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.tracing.AwsTracingRequestHandler;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.service.Retry;

import io.opentracing.Tracer;

@Component
public class AwsCloudFormationClient extends AwsClient {

    @Inject
    private Retry retry;

    @Inject
    private Tracer tracer;

    public AmazonCloudFormationClient createCloudFormationClient(AwsCredentialView awsCredential, String regionName) {
        AmazonCloudFormation cloudFormationClient = createCloudFormation(awsCredential, regionName);
        return new AmazonCloudFormationClient(proxy(cloudFormationClient, awsCredential, regionName), retry);
    }

    @VisibleForTesting
    AmazonCloudFormation createCloudFormation(AwsCredentialView awsCredential, String regionName) {
        AmazonCloudFormationClientBuilder clientBuilder = com.amazonaws.services.cloudformation.AmazonCloudFormationClient.builder()
                .withCredentials(getCredentialProvider(awsCredential))
                .withClientConfiguration(getDefaultClientConfiguration())
                .withRequestHandlers(new AwsTracingRequestHandler(tracer));
        getAwsEndpointProvider().setupEndpoint(clientBuilder, AmazonCloudFormation.ENDPOINT_PREFIX, regionName, awsCredential.isGovernmentCloudEnabled());
        return clientBuilder.build();
    }

    public AmazonAutoScalingClient createAutoScalingClient(AwsCredentialView awsCredential, String regionName) {
        AmazonAutoScalingClientBuilder clientBuilder = com.amazonaws.services.autoscaling.AmazonAutoScalingClient.builder()
                .withCredentials(getCredentialProvider(awsCredential))
                .withRequestHandlers(new AwsTracingRequestHandler(tracer))
                .withClientConfiguration(getDefaultClientConfiguration());
        getAwsEndpointProvider().setupEndpoint(clientBuilder, AmazonAutoScaling.ENDPOINT_PREFIX, regionName, awsCredential.isGovernmentCloudEnabled());
        return new AmazonAutoScalingClient(proxy(clientBuilder.build(), awsCredential, regionName), retry);
    }
}
