package com.sequenceiq.cloudbreak.client;

import static com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.AWSElbDnsTarget;
import static com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryResponse;
import static com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DnsTarget;
import static com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.IPs;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementGrpc;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementGrpc.PublicEndpointManagementBlockingStub;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CertificateSigningRequest;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.CreateDnsEntryRequest;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryRequest;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.DeleteDnsEntryResponse;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesRequest;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningRequest;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.PollCertificateSigningResponse;
import com.google.protobuf.ByteString;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;

import io.grpc.ManagedChannel;
import io.opentracing.Tracer;

public class ClusterDnsClient {

    private final ManagedChannel channel;

    private final String actorCrn;

    private final Tracer tracer;

    public ClusterDnsClient(ManagedChannel channel, String actorCrn, Tracer tracer) {
        this.channel = channel;
        this.actorCrn = actorCrn;
        this.tracer = tracer;
    }

    public String signCertificate(String requestId, String accountId, String environment, byte[] csr) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(accountId, "accountId should not be null.");
        CertificateSigningRequest.Builder requestBuilder = CertificateSigningRequest.newBuilder()
                .setAccountId(accountId)
                .setEnvironmentName(environment)
                .setCsr(ByteString.copyFrom(csr));

        return newStub(requestId).signCertificate(requestBuilder.build()).getWorkflowId();
    }

    public PollCertificateSigningResponse pollCertificateSigning(String requestId, String workflowId) {
        checkNotNull(requestId, "requestId should not be null.");
        final PollCertificateSigningRequest.Builder builder = PollCertificateSigningRequest
                .newBuilder()
                .setWorkflowId(workflowId);

        return newStub(requestId).pollCertificateSigning(builder.build());
    }

    public CreateDnsEntryResponse createDnsEntryWithIp(String requestId, String accountId, String endpoint, String environment, boolean wildcard,
            List<String> ips) {
        checkNotNull(requestId, "requestId should not be null.");

        DnsTarget dnsTarget = DnsTarget.newBuilder()
                .setTargetIPs(IPs.newBuilder().addAllIP(ips).build())
                .build();

        CreateDnsEntryRequest.Builder requestBuilder = CreateDnsEntryRequest.newBuilder()
                .setAddWildcard(wildcard)
                .setDnsTarget(dnsTarget)
                .setAccountId(accountId)
                .setEndpoint(endpoint)
                .setEnvironment(environment);

        return newStub(requestId).createDnsEntry(requestBuilder.build());
    }

    public DeleteDnsEntryResponse deleteDnsEntryWithIp(String requestId, String accountId, String endpoint, String environment, boolean wildcard,
            List<String> ips) {
        checkNotNull(requestId, "requestId should not be null.");
        DnsTarget dnsTarget = DnsTarget.newBuilder()
                .setTargetIPs(IPs.newBuilder().addAllIP(ips).build())
                .build();
        DeleteDnsEntryRequest.Builder requestBuilder = DeleteDnsEntryRequest.newBuilder()
                .setRemoveWildcard(wildcard)
                .setDnsTarget(dnsTarget)
                .setAccountId(accountId)
                .setEndpoint(endpoint)
                .setEnvironment(environment);
        return newStub(requestId).deleteDnsEntry(requestBuilder.build());
    }

    public CreateDnsEntryResponse createDnsEntryWithCloudDns(String requestId, String accountId, String endpoint,
            String environment, String cloudDns, String hostedZoneId) {
        checkNotNull(requestId, "requestId should not be null.");

        AWSElbDnsTarget awsElbDnsTarget = AWSElbDnsTarget.newBuilder()
            .setDnsName(cloudDns)
            .setHostedZoneId(hostedZoneId)
            .build();

        DnsTarget dnsTarget = DnsTarget.newBuilder()
            .setTargetAWSELBDns(awsElbDnsTarget)
            .build();

        CreateDnsEntryRequest.Builder requestBuilder = CreateDnsEntryRequest.newBuilder()
            .setDnsTarget(dnsTarget)
            .setAccountId(accountId)
            .setEndpoint(endpoint)
            .setEnvironment(environment);

        return newStub(requestId).createDnsEntry(requestBuilder.build());
    }

    public DeleteDnsEntryResponse deleteDnsEntryWithCloudDns(String requestId, String accountId, String endpoint,
            String environment, String cloudDns, String hostedZoneId) {
        checkNotNull(requestId, "requestId should not be null.");

        AWSElbDnsTarget awsElbDnsTarget = AWSElbDnsTarget.newBuilder()
            .setDnsName(cloudDns)
            .setHostedZoneId(hostedZoneId)
            .build();

        DnsTarget dnsTarget = DnsTarget.newBuilder()
            .setTargetAWSELBDns(awsElbDnsTarget)
            .build();

        DeleteDnsEntryRequest.Builder requestBuilder = DeleteDnsEntryRequest.newBuilder()
            .setDnsTarget(dnsTarget)
            .setAccountId(accountId)
            .setEndpoint(endpoint)
            .setEnvironment(environment);

        return newStub(requestId).deleteDnsEntry(requestBuilder.build());
    }

    public GenerateManagedDomainNamesResponse generateManagedDomainNames(String requestId, String environmentName, List<String> subDomains, String accountId) {
        checkNotNull(requestId, "requestId should not be null.");
        checkNotNull(environmentName, "environmentName should not be null.");
        checkNotNull(subDomains, "subDomain should not be null.");
        checkNotNull(accountId, "accountId should not be null.");

        GenerateManagedDomainNamesRequest.Builder requestBuilder = GenerateManagedDomainNamesRequest.newBuilder();
        requestBuilder.setEnvironmentName(environmentName);
        requestBuilder.setAccountId(accountId);
        requestBuilder.addAllSubdomains(subDomains);

        GenerateManagedDomainNamesRequest request = requestBuilder.build();
        return newStub(requestId).generateManagedDomainNames(request);
    }

    /**
     * Creates a new stub with the appropriate metadata injecting interceptors.
     *
     * @param requestId the request ID
     * @return the stub
     */
    private PublicEndpointManagementBlockingStub newStub(String requestId) {
        checkNotNull(requestId, "requestId should not be null.");
        return PublicEndpointManagementGrpc.newBlockingStub(channel)
                .withInterceptors(GrpcUtil.getTracingInterceptor(tracer),
                        new AltusMetadataInterceptor(requestId, actorCrn));
    }
}
