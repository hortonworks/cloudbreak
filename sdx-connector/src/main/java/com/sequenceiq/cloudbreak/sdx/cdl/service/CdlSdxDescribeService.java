package com.sequenceiq.cloudbreak.sdx.cdl.service;


import static com.sequenceiq.common.model.CloudStorageCdpService.DEFAULT_FS;
import static com.sequenceiq.common.model.CloudStorageCdpService.HIVE_METASTORE_EXTERNAL_WAREHOUSE;
import static com.sequenceiq.common.model.CloudStorageCdpService.HIVE_METASTORE_WAREHOUSE;
import static com.sequenceiq.common.model.CloudStorageCdpService.HIVE_REPLICA_WAREHOUSE;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.cdl.grpc.GrpcSdxCdlClient;
import com.sequenceiq.cloudbreak.sdx.common.grpc.GrpcServiceDiscoveryClient;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxAccessView;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxFileSystemView;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDescribeService;
import com.sequenceiq.cloudbreak.sdx.common.service.RdcViewFactory;
import com.sequenceiq.common.model.FileSystemType;

@Service
public class CdlSdxDescribeService extends AbstractCdlSdxService implements PlatformAwareSdxDescribeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdlSdxDescribeService.class);

    private static final String RANGER_ENDPOINT_INFO_NAME = "ranger";

    private static final String HIVE_ENDPOINT_INFO_NAME = "hive";

    private static final String HIVE_WAREHOUSE_SERVICE_CONFIG = "hive_warehouse_directory";

    private static final String HIVE_EXTERNAL_WAREHOUSE_SERVICE_CONFIG = "hive_warehouse_external_directory";

    private static final String HIVE_REPLICA_WAREHOUSE_SERVICE_CONFIG = "hive_repl_replica_functions_root_dir";

    @Inject
    private RdcViewFactory commonSdxDescribeService;

    @Inject
    private GrpcSdxCdlClient grpcClient;

    @Inject
    private GrpcServiceDiscoveryClient grpcServiceDiscoveryClient;

    @Override
    public Optional<String> getRemoteDataContext(String crn) {
        if (isEnabled(crn)) {
            try {
                return Optional.of(grpcServiceDiscoveryClient.getRemoteDataContext(crn));
            } catch (JsonProcessingException | InvalidProtocolBufferException e) {
                LOGGER.error(String.format("Json processing failed, thus we cannot query remote data context. CRN: %s.", crn), e);
            } catch (RuntimeException exception) {
                LOGGER.error(String.format("Not able to fetch the RDC for CDL from Service Discovery. CRN: %s.", crn), exception);
            }
        }
        throw new RuntimeException("Not able to fetch the RDC for CDL from Service Discovery");
    }

    @Override
    public Set<String> listSdxCrns(String environmentCrn) {
        if (isEnabled(environmentCrn)) {
            Set<String> sdxCrns = new HashSet<>();
            try {
                CdlCrudProto.ListDatalakesResponse datalakes = grpcClient.listDatalakes(environmentCrn, StringUtils.EMPTY);
                for (CdlCrudProto.DatalakeResponse datalakeResponse : datalakes.getDatalakeResponseList()) {
                    sdxCrns.add(datalakeResponse.getCrn());
                }
                return sdxCrns;
            } catch (RuntimeException exception) {
                LOGGER.error(String.format("Failed at listing CDL. CRN: %s.", environmentCrn), exception);
                return Collections.emptySet();
            }
        }
        return Set.of();
    }

    @Override
    public Optional<SdxBasicView> getSdxByEnvironmentCrn(String environmentCrn) {
        if (isEnabled(environmentCrn)) {
            try {
                CdlCrudProto.DatalakeResponse datalake = grpcClient.findDatalake(environmentCrn, StringUtils.EMPTY);
                CdlCrudProto.DescribeDatalakeResponse detailedCdl = grpcClient.describeDatalake(datalake.getCrn());
                CdlCrudProto.DescribeServicesResponse cdlServicesInfo = grpcClient.describeDatalakeServices(datalake.getCrn());
                return Optional.of(SdxBasicView.builder()
                        .withName(detailedCdl.getName())
                        .withCrn(detailedCdl.getCrn())
                        .withRuntime(detailedCdl.getRuntimeVersion())
                        .withRazEnabled(detailedCdl.getRangerRazEnabled())
                        .withCreated(detailedCdl.getCreated())
                        .withDbServerCrn(detailedCdl.getDatabaseDetails().getCrn())
                        .withFileSystemView(describeServicesResponseToFileSystem(cdlServicesInfo, detailedCdl.getCloudStorageBaseLocation()))
                        .withPlatform(TargetPlatform.CDL)
                        .build());
            } catch (RuntimeException exception) {
                LOGGER.error(String.format("Exception while fetching CRN for containerized datalake with Environment: %s.", environmentCrn), exception);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<SdxAccessView> getSdxAccessViewByEnvironmentCrn(String environmentCrn) {
        if  (isEnabled(environmentCrn)) {
            CdlCrudProto.DatalakeResponse datalake = grpcClient.findDatalake(environmentCrn, StringUtils.EMPTY);
            String rangerFqdn = grpcClient.describeDatalakeServices(datalake.getCrn())
                    .getEndpointsList().stream()
                    .filter(endpointInfo -> StringUtils.containsIgnoreCase(endpointInfo.getName(), RANGER_ENDPOINT_INFO_NAME))
                    .filter(endpointInfo -> !endpointInfo.getEndpointHostsList().isEmpty())
                    .map(endpointInfo -> {
                        try {
                            return new URI(endpointInfo.getEndpointHostsList().getFirst().getUri()).getHost();
                        } catch (Exception e) {
                            LOGGER.error("Couldn't parse URI for Ranger endpoint host.", e);
                            return StringUtils.EMPTY;
                        }
                    })
                    .findFirst()
                    .orElse(null);
            // here we are returning only with ranger FQDN since in case of CDL, term "cluster manager" is not applicable
            return Optional.of(SdxAccessView.builder().withRangerFqdn(rangerFqdn).build());
        }
        return Optional.empty();
    }

    @Override
    public Set<String> listSdxCrnsDetachedIncluded(String environmentCrn) {
        return listSdxCrns(environmentCrn);
    }

    private SdxFileSystemView describeServicesResponseToFileSystem(CdlCrudProto.DescribeServicesResponse servicesResponse, String cloudStorageLocation) {
        String fileSystemType = null;
        Map<String, String> fileSystemLocations = new HashMap<>();
        fileSystemLocations.put(DEFAULT_FS.name(), cloudStorageLocation);

        try {
            fileSystemType = determineFileSystemTypeFromLocation(cloudStorageLocation)
                    .map(Enum::name)
                    .orElseThrow(() -> new RuntimeException("File system type could not be found for cloud location " + cloudStorageLocation));

            List<CdlCrudProto.Config> hiveServiceConfigs = servicesResponse
                    .getEndpointsList().stream()
                    .filter(endpointInfo -> StringUtils.containsIgnoreCase(endpointInfo.getName(), HIVE_ENDPOINT_INFO_NAME))
                    .map(CdlCrudProto.EndpointInfo::getServiceConfigsList)
                    .findFirst()
                    .orElse(List.of());
            hiveServiceConfigs.forEach(config -> addHiveServiceLocationIfNeeded(config, fileSystemLocations));
        } catch (Exception e) {
            LOGGER.error("Failed to properly parse file system locations.", e);
        }

        return new SdxFileSystemView(fileSystemType, fileSystemLocations);
    }

    private Optional<FileSystemType> determineFileSystemTypeFromLocation(String cloudStorageLocation) {
        if (cloudStorageLocation != null) {
            for (FileSystemType probableFsType : FileSystemType.values()) {
                if (cloudStorageLocation.startsWith(probableFsType.getProtocol())) {
                    return Optional.of(probableFsType);
                }
            }
        }
        return Optional.empty();
    }

    private void addHiveServiceLocationIfNeeded(CdlCrudProto.Config config, Map<String, String> fileSystemLocations) {
        switch (config.getKey()) {
            case HIVE_WAREHOUSE_SERVICE_CONFIG -> fileSystemLocations.put(HIVE_METASTORE_WAREHOUSE.name(), config.getValue());
            case HIVE_EXTERNAL_WAREHOUSE_SERVICE_CONFIG -> fileSystemLocations.put(HIVE_METASTORE_EXTERNAL_WAREHOUSE.name(), config.getValue());
            case HIVE_REPLICA_WAREHOUSE_SERVICE_CONFIG -> fileSystemLocations.put(HIVE_REPLICA_WAREHOUSE.name(), config.getValue());
            default -> LOGGER.info("Ignoring Hive service location present in filesystem.");
        }
    }
}