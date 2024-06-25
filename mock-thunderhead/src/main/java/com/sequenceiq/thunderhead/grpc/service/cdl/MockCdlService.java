package com.sequenceiq.thunderhead.grpc.service.cdl;

import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.cdlcrud.CdlCrudGrpc;
import com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.thunderhead.entity.Cdl;
import com.sequenceiq.thunderhead.repository.CdlRespository;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

@Service
public class MockCdlService extends CdlCrudGrpc.CdlCrudImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockCdlService.class);

    private static final String CDL_NAME = "cdl";

    private static final String CDL_RUNTIME = "7.2.18";

    private static final String CDL_SHAPE = "CONTAINERIZED";

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Inject
    private CdlRespository cdlRespository;

    @Override
    public void createDatalake(CdlCrudProto.CreateDatalakeRequest request, StreamObserver<CdlCrudProto.CreateDatalakeResponse> responseObserver) {
        // request only contains environment name, not crn, so as a hack, we can use name field to define env crn
        Crn envCrn = Crn.safeFromString(request.getEnvironmentName());
        Optional<Cdl> cdlByEnvCrn = cdlRespository.findByEnvironmentCrn(envCrn.toString());
        Crn cdlCrn;
        if (cdlByEnvCrn.isEmpty()) {
            cdlCrn = regionAwareCrnGenerator.generateCrn(CrnResourceDescriptor.CDL, UUID.randomUUID().toString(), envCrn.getAccountId());
            Cdl cdl = new Cdl();
            cdl.setCrn(cdlCrn.toString());
            cdl.setEnvironmentCrn(envCrn.toString());
            cdlRespository.save(cdl);
        } else {
            cdlCrn = Crn.safeFromString(cdlByEnvCrn.get().getCrn());
        }
        responseObserver.onNext(CdlCrudProto.CreateDatalakeResponse.newBuilder()
                .setCrn(cdlCrn.toString())
                .setEnvironmentCrn(envCrn.toString())
                .setStatus(CdlCrudProto.StatusType.Value.RUNNING.name())
                .setDatalakeName(CDL_NAME)
                .setEnableRangerRaz(false)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void findDatalake(CdlCrudProto.FindDatalakeRequest request, StreamObserver<CdlCrudProto.DatalakeResponse> responseObserver) {
        Crn envCrn = Crn.safeFromString(request.getEnvironment());
        Optional<Cdl> cdlByEnvironmentCrn = cdlRespository.findByEnvironmentCrn(envCrn.toString());
        if (cdlByEnvironmentCrn.isPresent()) {
            responseObserver.onNext(getDatalakeResponse(cdlByEnvironmentCrn.get()));
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(Status.NOT_FOUND.asException());
        }
    }

    @Override
    public void describeDatalake(CdlCrudProto.DescribeDatalakeRequest request, StreamObserver<CdlCrudProto.DescribeDatalakeResponse> responseObserver) {
        Crn crn = Crn.safeFromString(request.getDatalake());
        Optional<Cdl> cdlByCrn = cdlRespository.findByCrn(crn.toString());
        if (cdlByCrn.isPresent()) {
            Cdl cdl = cdlByCrn.get();
            CdlCrudProto.DatabaseInfo databaseDetails = CdlCrudProto.DatabaseInfo.newBuilder()
                    .setCrn(cdl.getDatabaseServerCrn())
                    .build();
            responseObserver.onNext(CdlCrudProto.DescribeDatalakeResponse.newBuilder()
                    .setCrn(cdl.getCrn())
                    .setAccountID(crn.getAccountId())
                    .setStatus(CdlCrudProto.StatusType.Value.RUNNING)
                    .setShape(CDL_SHAPE)
                    .setRuntimeVersion(CDL_RUNTIME)
                    .setName(CDL_NAME)
                    .setRangerRazEnabled(false)
                    .setEnvironmentCrn(cdl.getEnvironmentCrn())
                    .setDatabaseDetails(databaseDetails)
                    .build());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(Status.NOT_FOUND.asException());
        }
    }

    @Override
    public void listDatalakes(CdlCrudProto.ListDatalakesRequest request, StreamObserver<CdlCrudProto.ListDatalakesResponse> responseObserver) {
        Crn envCrn = Crn.safeFromString(request.getEnvironment());
        Optional<Cdl> cdlByEnvironmentCrn = cdlRespository.findByEnvironmentCrn(envCrn.toString());
        if (cdlByEnvironmentCrn.isPresent()) {
            responseObserver.onNext(CdlCrudProto.ListDatalakesResponse.newBuilder()
                    .addDatalakeResponse(getDatalakeResponse(cdlByEnvironmentCrn.get()))
                    .build());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(Status.NOT_FOUND.asException());
        }
    }

    @Override
    public void deleteDatalake(CdlCrudProto.DeleteDatalakeRequest request, StreamObserver<CdlCrudProto.DeleteDatalakeResponse> responseObserver) {
        Crn crn = Crn.safeFromString(request.getDatalake());
        Optional<Cdl> cdlByCrn = cdlRespository.findByCrn(crn.toString());
        if (cdlByCrn.isPresent()) {
            Cdl cdl = cdlByCrn.get();
            cdlRespository.delete(cdl);
            responseObserver.onNext(CdlCrudProto.DeleteDatalakeResponse.newBuilder()
                    .setCrn(cdl.getCrn())
                    .setStatus(CdlCrudProto.StatusType.Value.DELETED.name())
                    .build());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(Status.NOT_FOUND.asException());
        }
    }

    private CdlCrudProto.DatalakeResponse getDatalakeResponse(Cdl cdl) {
        return CdlCrudProto.DatalakeResponse.newBuilder()
                .setCrn(cdl.getCrn())
                .setStatus(CdlCrudProto.StatusType.Value.RUNNING.name())
                .setName(CDL_NAME)
                .setEnvironmentCrn(cdl.getEnvironmentCrn())
                .setRangerRazEnabled(false)
                .build();
    }
}
