package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.common.model.Architecture;

@Service
public class VmAdvisor {

    private static final Logger LOGGER = LoggerFactory.getLogger(VmAdvisor.class);

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    public CloudVmTypes recommendVmTypes(BlueprintTextProcessor blueprintTextProcessor, String region, String platformVariant,
            CdpResourceType cdpResourceType, Credential credential, Architecture architecture) {
        String version = blueprintTextProcessor.getVersion().orElse("");
        if (!isVersionNewerOrEqualThanLimited(version, CLOUDERA_STACK_VERSION_7_3_1) && architecture == Architecture.ARM64) {
            LOGGER.debug("Arm64 architecture is not supported in this version {}.", version);
            return new CloudVmTypes();
        }
        return cloudParameterService.getVmTypesV2(
                extendedCloudCredentialConverter.convert(credential),
                region,
                platformVariant,
                cdpResourceType,
                Maps.newHashMap(Map.of(CloudResourceAdvisor.ARCHITECTURE, architecture.getName())));
    }
}
