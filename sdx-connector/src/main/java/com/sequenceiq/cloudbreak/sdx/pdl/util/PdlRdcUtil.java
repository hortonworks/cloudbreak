package com.sequenceiq.cloudbreak.sdx.pdl.util;

import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HDFS_SERVICE;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HIVE_SERVICE;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HdfsNameNode.HDFS_NAMENODE_NAMESERVICE;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HdfsNameNode.HDFS_NAMENODE_ROLE_TYPE;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.Hive.HIVE_WAREHOUSE_DIRECTORY;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.Hive.HIVE_WAREHOUSE_EXTERNAL_DIRECTORY;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import com.cloudera.api.swagger.model.ApiEndPoint;
import com.cloudera.api.swagger.model.ApiMapEntry;
import com.cloudera.api.swagger.model.ApiRemoteDataContext;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sequenceiq.cloudbreak.sdx.RdcView;
import com.sequenceiq.cloudbreak.template.TemplateEndpoint;
import com.sequenceiq.cloudbreak.template.TemplateRoleConfig;

@Component
public class PdlRdcUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdlRdcUtil.class);

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().
            defaultSetterInfo(JsonSetter.Value.construct(Nulls.AS_EMPTY, Nulls.AS_EMPTY)).build();

    private static final Map<String, List<String>> SERVICE_CONFIGS_BY_SERVICE_TYPE = Map.of(
            HIVE_SERVICE.toUpperCase(), List.of(HIVE_WAREHOUSE_DIRECTORY, HIVE_WAREHOUSE_EXTERNAL_DIRECTORY)
    );

    public ApiRemoteDataContext parseRemoteDataContext(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, ApiRemoteDataContext.class);
        } catch (JsonProcessingException e) {
            LOGGER.error("Json processing failed, thus we cannot query remote data context", e);
            throw new RuntimeException("Failed to process remote data context. Please contact Cloudera support to get this resolved.");
        }
    }

    public String remoteDataContextToJson(String crn, ApiRemoteDataContext apiRemoteDataContext) {
        try {
            return OBJECT_MAPPER.writeValueAsString(apiRemoteDataContext);
        } catch (JsonProcessingException e) {
            LOGGER.error("Json processing failed, thus we cannot query remote data context. CRN: {}.", crn, e);
            throw new RuntimeException("Failed to process remote data context. Please contact Cloudera support to get this resolved.");
        }
    }

    public RdcView extendRdcView(RdcView rdcView, DescribeDatalakeServicesResponse datalakeServices) {
        if (!ObjectUtils.isEmpty(datalakeServices.getApplications())) {
            if (datalakeServices.getApplications().containsKey(HDFS_SERVICE)) {
                LOGGER.info("Extending RDC with HDFS config");
                Map<String, String> hdfsConfig = Objects.requireNonNullElse(datalakeServices.getApplications().get(HDFS_SERVICE).getConfig(), Map.of());
                extendRdcWithHdfsConfig(rdcView, hdfsConfig);
            }
        }
        return rdcView;
    }

    private void extendRdcWithHdfsConfig(RdcView rdcView, Map<String, String> hdfsConfig) {
        String defaultFs = hdfsConfig.get("fs.defaultFS");
        String nameServices = hdfsConfig.get("dfs_nameservices");
        if (!ObjectUtils.isEmpty(nameServices)) {
            Set<TemplateRoleConfig> roleConfigs = new HashSet<>();
            String defaultNameService = getDefaultNameService(defaultFs, nameServices);
            roleConfigs.add(new TemplateRoleConfig(HDFS_SERVICE, HDFS_NAMENODE_ROLE_TYPE, HDFS_NAMENODE_NAMESERVICE, defaultNameService));
            hdfsConfig.entrySet().stream()
                    .filter(configEntry -> isHdfsNamenodeConfig(configEntry.getKey()))
                    .map(configEntry -> new TemplateRoleConfig(HDFS_SERVICE, HDFS_NAMENODE_ROLE_TYPE, configEntry.getKey(), configEntry.getValue()))
                    .forEach(roleConfigs::add);
            LOGGER.info("Extending RDC with HDFS namenode role configs: {}", roleConfigs.stream().map(TemplateRoleConfig::key).collect(Collectors.joining()));
            rdcView.extendRoleConfigs(roleConfigs);
        }
        LOGGER.info("Extending RDC with HDFS namenode endpoint");
        rdcView.extendEndpoints(Set.of(new TemplateEndpoint(HDFS_SERVICE, HDFS_NAMENODE_ROLE_TYPE, defaultFs)));

        extendRdcServiceConfigsToAbsolutePath(rdcView, defaultFs);
    }

    private String getDefaultNameService(String defaultFs, String nameServices) {
        if (ObjectUtils.isEmpty(defaultFs)) {
            LOGGER.debug("No default filesystem found, using the first nameservice");
            return nameServices.split(",")[0];
        }
        LOGGER.debug("Finding nameservice corresponding to default filesystem");
        return Arrays.stream(nameServices.split(","))
                .filter(defaultFs::contains)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Failed to determine default nameservice. Please contact Cloudera support to get this resolved."));
    }

    private boolean isHdfsNamenodeConfig(String configKey) {
        return configKey.startsWith("dfs.ha.namenodes.") || configKey.startsWith("dfs.namenode.");
    }

    private void extendRdcServiceConfigsToAbsolutePath(RdcView rdcView, String defaultFs) {
        if (rdcView.getRemoteDataContext().isPresent()) {
            ApiRemoteDataContext apiRemoteDataContext = parseRemoteDataContext(rdcView.getRemoteDataContext().get());
            for (ApiEndPoint endpoint : apiRemoteDataContext.getEndPoints()) {
                String serviceType = endpoint.getServiceType().toUpperCase();
                if (SERVICE_CONFIGS_BY_SERVICE_TYPE.containsKey(serviceType)) {
                    List<String> serviceConfigs = SERVICE_CONFIGS_BY_SERVICE_TYPE.get(serviceType);
                    for (ApiMapEntry serviceConfig : endpoint.getServiceConfigs()) {
                        if (serviceConfigs.contains(serviceConfig.getKey()) && !serviceConfig.getValue().matches("^.+://.*")) {
                            LOGGER.debug("Extending RDC config {} of service {} value to absolute path", serviceConfig.getKey(), serviceType);
                            serviceConfig.setValue(defaultFs + serviceConfig.getValue());
                        }
                    }
                }
            }
            rdcView.updateRemoteDataContext(remoteDataContextToJson(rdcView.getStackCrn(), apiRemoteDataContext));
        }
    }
}
