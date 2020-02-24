package com.sequenceiq.datalake.configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    public static final int RUNTIME_GROUP = 1;

    public static final int CLOUDPLATFORM_GROUP = 2;

    public static final int CLUSTERSHAPE_GROUP = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPConfig.class);

    @Value("${datalake.unsupported.runtimes:}")
    private Set<String> unsupportedRuntimes;

    @Bean
    public Map<CDPConfigKey, StackV4Request> cdpStackRequests() {
        Map<CDPConfigKey, StackV4Request> cpdStackRequests = new HashMap<>();
        PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = pathMatchingResourcePatternResolver.getResources("classpath:runtime/*/*/*.json");
            for (Resource resource : resources) {
                Matcher matcher = Pattern.compile(".*/runtime/(.*)/(.*)/(.*).json").matcher(resource.getURL().getPath());
                if (matcher.find()) {
                    String runtimeVersion = matcher.group(RUNTIME_GROUP);
                    if (!unsupportedRuntimes.contains(runtimeVersion)) {
                        CloudPlatform cloudPlatform = CloudPlatform.valueOf(matcher.group(CLOUDPLATFORM_GROUP).toUpperCase());
                        SdxClusterShape sdxClusterShape = SdxClusterShape.valueOf(matcher.group(CLUSTERSHAPE_GROUP).toUpperCase());
                        CDPConfigKey cdpConfigKey = new CDPConfigKey(cloudPlatform, sdxClusterShape, runtimeVersion);
                        String templateString = IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8.name());
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
