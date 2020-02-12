package com.sequenceiq.datalake.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.service.sdx.CDPConfigKey;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Configuration
public class CDPConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPConfig.class);

    @Bean
    public Map<CDPConfigKey, StackV4Request> cdpStackRequests() {
        Map<CDPConfigKey, StackV4Request> cpdStackRequests = new HashMap<>();
        PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
        try {
            Resource[] resources = pathMatchingResourcePatternResolver.getResources("classpath*:/runtime/*");
            for (Resource resource : resources) {
                String cdpVersion = resource.getFile().getName();
                File[] cloudPlatformDirs = resource.getFile().listFiles();
                for (File cloudPlatformFile : cloudPlatformDirs) {
                    CloudPlatform cloudPlatform = CloudPlatform.valueOf(cloudPlatformFile.getName().toUpperCase());
                    for (File dutyFile : cloudPlatformFile.listFiles()) {
                        SdxClusterShape sdxClusterShape = SdxClusterShape.valueOf(dutyFile.getName().split(".json")[0].toUpperCase());
                        CDPConfigKey cdpConfigKey = new CDPConfigKey(cloudPlatform, sdxClusterShape, cdpVersion);
                        String templateString = FileUtils.readFileToString(dutyFile, Charset.defaultCharset());
                        StackV4Request stackV4Request = JsonUtil.readValue(templateString, StackV4Request.class);
                        cpdStackRequests.put(cdpConfigKey, stackV4Request);
                    }
                }
            }
            LOGGER.info("Cdp configs for datalakes: {}", cpdStackRequests);
            return cpdStackRequests;
        } catch (IOException e) {
            LOGGER.error("Can't read CDP template files", e);
            throw new IllegalStateException("Can't read CDP template files", e);
        }
    }

}
