package com.sequenceiq.thunderhead.grpc.service.pem;

import static com.google.common.base.Preconditions.checkArgument;

import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementGrpc;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto;
import com.cloudera.thunderhead.service.publicendpointmanagement.PublicEndpointManagementProto.GenerateManagedDomainNamesResponse;
import com.google.common.base.Strings;
import com.google.protobuf.ProtocolStringList;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.dns.LegacyEnvironmentNameBasedDomainNameProvider;
import com.sequenceiq.thunderhead.entity.PublicCertGen;
import com.sequenceiq.thunderhead.grpc.service.auth.MockUserManagementService;
import com.sequenceiq.thunderhead.repository.PublicCertGenRepository;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

@Component
public class MockPublicEndpointManagementService extends PublicEndpointManagementGrpc.PublicEndpointManagementImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockPublicEndpointManagementService.class);

    @Inject
    private LegacyEnvironmentNameBasedDomainNameProvider environmentNameBasedDomainNameProvider;

    @Inject
    private PublicCertGenRepository publicCertGenRepository;

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

    @Override
    public void signCertificate(PublicEndpointManagementProto.CertificateSigningRequest request,
            StreamObserver<PublicEndpointManagementProto.CertificateSigningResponse> responseObserver) {
        String workflowId = UUID.randomUUID().toString();

        PublicCertGen publicCertGen = new PublicCertGen();
        publicCertGen.setWorkFlowId(workflowId);
        publicCertGen.setCrn(request.getClusterCrn());
        publicCertGen.setEnvironmentName(request.getEnvironmentName());
        publicCertGen.setCsr(Base64.getEncoder().encodeToString(request.getCsr().toByteArray()));
        publicCertGenRepository.save(publicCertGen);
        PublicEndpointManagementProto.CertificateSigningResponse response = PublicEndpointManagementProto.CertificateSigningResponse.newBuilder()
                .setWorkflowId(workflowId)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void pollCertificateSigning(PublicEndpointManagementProto.PollCertificateSigningRequest request,
            StreamObserver<PublicEndpointManagementProto.PollCertificateSigningResponse> responseObserver) {
        Optional<PublicCertGen> byWorkFlowId = publicCertGenRepository.findByWorkFlowId(request.getWorkflowId());
        if (byWorkFlowId.isPresent()) {
            try {
                byte[] csrBytes = Base64.getDecoder().decode(byWorkFlowId.get().getCsr());
                PKCS10CertificationRequest csr = new PKCS10CertificationRequest(csrBytes);
                LOGGER.info("Generate certificate by CSR: {}", PkiUtil.getPEMEncodedCSR(csr));
                X509Certificate cert = PkiUtil.certByCsr(csr, "cloudera", PkiUtil.generateKeypair(), 10);
                String certString = PkiUtil.convert(cert);
                LOGGER.info("Generated certificate: {}", certString);
                PublicEndpointManagementProto.PollCertificateSigningResponse response =
                        PublicEndpointManagementProto.PollCertificateSigningResponse.newBuilder()
                                .addCertificates(certString)
                                .setStatus(PublicEndpointManagementProto.PollCertificateSigningResponse.SigningStatus.SUCCEEDED)
                                .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                publicCertGenRepository.delete(byWorkFlowId.get());
            } catch (Exception e) {
                LOGGER.error("Failure during certificate generation, ", e);
                responseObserver.onError(Status.INTERNAL.withCause(e).asException());
            }
        } else {
            responseObserver.onError(Status.INTERNAL.withDescription("CSR not found in mock database!").asException());
        }
    }

    @Override
    public void createDnsEntry(PublicEndpointManagementProto.CreateDnsEntryRequest request,
            StreamObserver<PublicEndpointManagementProto.CreateDnsEntryResponse> responseObserver) {
        responseObserver.onNext(PublicEndpointManagementProto.CreateDnsEntryResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteDnsEntry(PublicEndpointManagementProto.DeleteDnsEntryRequest request,
            StreamObserver<PublicEndpointManagementProto.DeleteDnsEntryResponse> responseObserver) {
        responseObserver.onNext(PublicEndpointManagementProto.DeleteDnsEntryResponse.newBuilder().build());
        responseObserver.onCompleted();
    }
}
