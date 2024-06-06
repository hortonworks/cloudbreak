package com.sequenceiq.thunderhead.grpc.service.servicediscovery;

import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_HOST;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_NAME;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_PASSWORD;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_PORT;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_USER;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.cloudera.cdp.servicediscovery.ServiceDiscoveryGrpc;
import com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto;
import com.sequenceiq.thunderhead.entity.Cdl;
import com.sequenceiq.thunderhead.repository.CdlRespository;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

@Service
public class MockServiceDiscoveryService extends ServiceDiscoveryGrpc.ServiceDiscoveryImplBase {

    private static final String HIVE_DB_PORT = "5432";

    private static final String CLUSTER_VERSION = "CDH 7.2.18";

    private static final String CDL_ENDPOINT_ID = "cdl";

    private static final String DB_TYPE = "CDL_HIVE";

    private static final String HIVE_SERVICE_TYPE = "HIVE";

    @Inject
    private CdlRespository cdlRespository;

    @Override
    public void describeDatalakeAsApiRemoteDataContext(ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextRequest request,
            StreamObserver<ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextResponse> responseObserver) {
        Optional<Cdl> cdlByCrn = cdlRespository.findByCrn(request.getDatalake());
        if (cdlByCrn.isPresent()) {
            Cdl cdl = cdlByCrn.get();
            responseObserver.onNext(ServiceDiscoveryProto.DescribeDatalakeAsApiRemoteDataContextResponse.newBuilder()
                    .setContext(ServiceDiscoveryProto.ApiRemoteDataContext.newBuilder()
                            .setClusterVersion(CLUSTER_VERSION)
                            .setEndPointId(CDL_ENDPOINT_ID)
                            .addEndPoints(ServiceDiscoveryProto.ApiEndPoint.newBuilder()
                                    .setName(DB_TYPE)
                                    .addServiceConfigs(mapEntry(HIVE_METASTORE_DATABASE_PORT, HIVE_DB_PORT))
                                    .addServiceConfigs(mapEntry(HIVE_METASTORE_DATABASE_HOST, cdl.getHmsDatabaseHost()))
                                    .addServiceConfigs(mapEntry(HIVE_METASTORE_DATABASE_NAME, cdl.getHmsDatabaseName()))
                                    .addServiceConfigs(mapEntry(HIVE_METASTORE_DATABASE_PASSWORD, cdl.getHmsDatabasePassword()))
                                    .addServiceConfigs(mapEntry(HIVE_METASTORE_DATABASE_USER, cdl.getHmsDatabaseUser()))
                                    .setServiceType(HIVE_SERVICE_TYPE)
                                    .build())
                            .build())
                    .build());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(Status.NOT_FOUND.asException());
        }
    }

    private ServiceDiscoveryProto.ApiMapEntry mapEntry(String key, String value) {
        return ServiceDiscoveryProto.ApiMapEntry.newBuilder().setKey(key).setValue(value).build();
    }
}
