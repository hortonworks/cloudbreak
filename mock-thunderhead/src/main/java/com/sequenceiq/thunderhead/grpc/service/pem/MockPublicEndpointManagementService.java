package com.sequenceiq.thunderhead.grpc.service.pem;

import static com.google.common.base.Preconditions.checkArgument;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementGrpc;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse;
import com.google.common.base.Strings;
import com.google.protobuf.ProtocolStringList;
import com.sequenceiq.cloudbreak.dns.LegacyEnvironmentNameBasedDomainNameProvider;
import com.sequenceiq.thunderhead.grpc.service.auth.MockUserManagementService;

import io.grpc.stub.StreamObserver;

@Component
public class MockPublicEndpointManagementService extends PublicEndpointManagementGrpc.PublicEndpointManagementImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockPublicEndpointManagementService.class);

    @Inject
    private LegacyEnvironmentNameBasedDomainNameProvider environmentNameBasedDomainNameProvider;

    @Override
    public void generateManagedDomainNames(PublicEndpointManagementProto.GenerateManagedDomainNamesRequest request,
            StreamObserver<GenerateManagedDomainNamesResponse> responseObserver) {
        ProtocolStringList subDomains = request.getSubdomainsList();
        String environmentName = request.getEnvironmentName();
        String accountId = request.getAccountId();
        checkArgument(!Strings.isNullOrEmpty(environmentName));
        checkArgument(!Strings.isNullOrEmpty(accountId));
        checkArgument(!subDomains.isEmpty());
        LOGGER.info("Generating mock managed domain for environment: '{}', accountid: '{}' with provided domains: '{}', ", environmentName, accountId,
                String.join(",", subDomains));

        String generatedManagedDomain = environmentNameBasedDomainNameProvider.getDomainName(environmentName, MockUserManagementService.ACCOUNT_SUBDOMAIN);

        GenerateManagedDomainNamesResponse response = GenerateManagedDomainNamesResponse.newBuilder()
                .putDomains("*", generatedManagedDomain)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
