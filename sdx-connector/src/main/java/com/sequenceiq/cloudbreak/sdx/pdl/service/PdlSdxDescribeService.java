package com.sequenceiq.cloudbreak.sdx.pdl.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiRemoteDataContext;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesRequest;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.environments2api.model.Application;
import com.cloudera.thunderhead.service.environments2api.model.Environment;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateResponse;
import com.cloudera.thunderhead.service.environments2api.model.Instance;
import com.cloudera.thunderhead.service.environments2api.model.PrivateDatalakeDetails;
import com.cloudera.thunderhead.service.environments2api.model.PvcEnvironmentDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.sdx.RdcView;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxAccessView;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxFileSystemView;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxDescribeService;
import com.sequenceiq.cloudbreak.sdx.pdl.util.PdlRdcUtil;
import com.sequenceiq.common.model.CloudStorageCdpService;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.remoteenvironment.api.v1.environment.model.DescribeRemoteEnvironment;

@Service
public class PdlSdxDescribeService extends AbstractPdlSdxService implements PlatformAwareSdxDescribeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdlSdxDescribeService.class);

    @Inject
    private PdlRdcUtil pdlRdcUtil;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Override
    public Optional<String> getRemoteDataContext(String crn) {
        try {
            DescribeRemoteEnvironment describeRemoteEnvironment = getRemoteEnvironmentRequest(crn);
            DescribeDatalakeAsApiRemoteDataContextResponse remoteDataContext = getRemoteEnvironmentEndPoint().getRdcByCrn(describeRemoteEnvironment);
            String parsedJson = JsonUtil.writeValueAsString(remoteDataContext.getContext());
            ApiRemoteDataContext apiRemoteDataContext = pdlRdcUtil.parseRemoteDataContext(parsedJson);
            return Optional.of(pdlRdcUtil.remoteDataContextToJson(crn, apiRemoteDataContext));
        } catch (JsonProcessingException e) {
            LOGGER.error("Json processing failed, thus we cannot query remote data context. CRN: {}.", crn, e);
            throw new RuntimeException(String.format("Not able to process the RDC for PDL %s: %s.", crn, e.getMessage()), e);
        } catch (RuntimeException exception) {
            String message = webApplicationExceptionMessageExtractor.getErrorMessage(exception);
            LOGGER.error("Not able to fetch the RDC for PDL {}: {}.", crn, message, exception);
            throw new RuntimeException(String.format("Not able to fetch the RDC for PDL %s: %s", crn, message), exception);
        }
    }

    @Override
    public RdcView extendRdcView(RdcView rdcView) {
        DescribeDatalakeServicesResponse datalakeServices = getDatalakeServicesByCrn(rdcView.getStackCrn());
        return pdlRdcUtil.extendRdcView(rdcView, datalakeServices);
    }

    private DescribeDatalakeServicesResponse getDatalakeServicesByCrn(String crn) {
        try {
            DescribeDatalakeServicesRequest request = new DescribeDatalakeServicesRequest().clusterid(crn);
            return getRemoteEnvironmentEndPoint().getDatalakeServicesByCrn(request);
        } catch (RuntimeException exception) {
            String message = webApplicationExceptionMessageExtractor.getErrorMessage(exception);
            LOGGER.error("Not able to fetch the datalake services for PDL {}: {}.", crn, message, exception);
            throw new RuntimeException(String.format("Not able to fetch the datalake services for PDL %s: %s", crn, message), exception);
        }
    }

    @Override
    public Set<String> listSdxCrns(String environmentCrn) {
        try {
            String pvcCrn = getPrivateCloudEnvCrn(environmentCrn).orElse(null);
            LOGGER.info("Private Cloud CRN is {}", pvcCrn);
            if (StringUtils.isNotBlank(pvcCrn)) {
                return Set.of(pvcCrn);
            }
        } catch (Exception e) {
            LOGGER.info("No Private Cloud environment found with CRN: {}", environmentCrn, e);
        }
        return Set.of();
    }

    @Override
    public Optional<SdxBasicView> getSdxByEnvironmentCrn(String environmentCrn) {
        try {
            Environment environment = getPrivateEnvForPublicEnv(environmentCrn);
            if (environment != null && environment.getPvcEnvironmentDetails() != null
                    && environment.getPvcEnvironmentDetails().getPrivateDatalakeDetails() != null) {
                PrivateDatalakeDetails privateDatalakeDetails = environment.getPvcEnvironmentDetails().getPrivateDatalakeDetails();
                return Optional.of(SdxBasicView.builder()
                        .withName(privateDatalakeDetails.getDatalakeName())
                        .withCrn(environment.getCrn())
                        .withRuntime(StringUtils.substringBefore(environment.getCdpRuntimeVersion(), "-"))
                        .withRazEnabled(privateDatalakeDetails.getEnableRangerRaz())
                        .withCreated(privateDatalakeDetails.getCreationTimeEpochMillis())
                        .withPlatform(TargetPlatform.PDL)
                        .build());
            }
        } catch (RuntimeException exception) {
            LOGGER.error(String.format("Exception while fetching CRN for private datalake with Environment: %s.", environmentCrn), exception);
            if (exception instanceof BadRequestException) {
                throw exception;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<SdxFileSystemView> getSdxFileSystemViewByEnvironmentCrn(String environmentCrn) {
        Environment environment = getPrivateEnvForPublicEnv(environmentCrn);
        DescribeDatalakeServicesResponse datalakeServices = getDatalakeServicesByCrn(environment.getCrn());
        String defaultFs = Optional.ofNullable(datalakeServices.getApplications())
                .map(applications -> applications.get("HDFS"))
                .map(com.cloudera.cdp.servicediscovery.model.Application::getConfig)
                .map(configs -> configs.get("fs.defaultFS"))
                .orElse(null);
        if (StringUtils.isEmpty(defaultFs)) {
            LOGGER.warn("Can not find fs.defaultFS config for {}, skipping filesystem configuration", environment.getCrn());
            return Optional.empty();
        }

        Map<String, String> sharedFileSystemLocationsByService = new HashMap<>();
        sharedFileSystemLocationsByService.put(CloudStorageCdpService.DEFAULT_FS.name(), defaultFs);

        Map<String, String> hiveConfig = Optional.ofNullable(environment.getPvcEnvironmentDetails())
                .map(PvcEnvironmentDetails::getApplications)
                .map(applications -> applications.get("HIVE"))
                .map(Application::getConfig)
                .orElse(Map.of());
        String hiveWarehouseDirectory = hiveConfig.get("hive_warehouse_directory");
        if (StringUtils.isNotEmpty(hiveWarehouseDirectory)) {
            sharedFileSystemLocationsByService.put(
                    CloudStorageCdpService.HIVE_METASTORE_WAREHOUSE.name(),
                    getAbsolutePath(hiveWarehouseDirectory, defaultFs));
        }
        String hiveWarehouseExternalDirectory = hiveConfig.get("hive_warehouse_external_directory");
        if (StringUtils.isNotEmpty(hiveWarehouseExternalDirectory)) {
            sharedFileSystemLocationsByService.put(
                    CloudStorageCdpService.HIVE_METASTORE_EXTERNAL_WAREHOUSE.name(),
                    getAbsolutePath(hiveWarehouseExternalDirectory, defaultFs));
        }
        return Optional.of(new SdxFileSystemView(FileSystemType.HDFS.name(), sharedFileSystemLocationsByService));
    }

    private String getAbsolutePath(String path, String defaultHost) {
        return path.startsWith("/") ? (defaultHost + path) : path;
    }

    @Override
    public Optional<SdxAccessView> getSdxAccessViewByEnvironmentCrn(String environmentCrn) {
        Environment environment = getPrivateEnvForPublicEnv(environmentCrn);
        if (environment != null && environment.getPvcEnvironmentDetails() != null
                && environment.getPvcEnvironmentDetails().getPrivateDatalakeDetails() != null) {
            PrivateDatalakeDetails privateDatalakeDetails = environment.getPvcEnvironmentDetails().getPrivateDatalakeDetails();
            return Optional.of(SdxAccessView.builder().withRangerFqdn(
                            StringUtils.isNotBlank(privateDatalakeDetails.getCmFQDN()) ? privateDatalakeDetails.getCmFQDN() : privateDatalakeDetails.getCmIP())
                    .build());
        }
        return Optional.empty();
    }

    @Override
    public Set<String> listSdxCrnsDetachedIncluded(String environmentCrn) {
        return listSdxCrns(environmentCrn);
    }

    @Override
    public Optional<String> getCACertsForEnvironment(String environmentCrn) {
        String pvcCrn = getPrivateCloudEnvCrn(environmentCrn).orElse(null);
        try {
            GetRootCertificateResponse response = getRemoteEnvironmentEndPoint().getRootCertificateByCrn(pvcCrn);
            return Optional.ofNullable(response).map(GetRootCertificateResponse::getContents);
        } catch (RuntimeException exception) {
            String message = webApplicationExceptionMessageExtractor.getErrorMessage(exception);
            LOGGER.error("Not able to fetch CA certs for PDL {}: {}.", pvcCrn, message, exception);
            throw new RuntimeException(String.format("Not able to fetch CA certs for PDL %s: %s", pvcCrn, message), exception);
        }
    }

    @Override
    public Set<String> getSdxDomains(String environmentCrn) {
        Environment environment = getPrivateEnvForPublicEnv(environmentCrn);
        return Optional.ofNullable(environment.getPvcEnvironmentDetails())
                .map(PvcEnvironmentDetails::getPrivateDatalakeDetails)
                .stream()
                .map(PrivateDatalakeDetails::getInstances)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(Instance::getDiscoveryFQDN)
                .filter(Objects::nonNull)
                .map(this::getDomainFromFqdn)
                .collect(Collectors.toSet());
    }

    private String getDomainFromFqdn(String instanceFqdn) {
        int firstDot = instanceFqdn.indexOf('.');
        return instanceFqdn.substring(firstDot + 1);
    }
}