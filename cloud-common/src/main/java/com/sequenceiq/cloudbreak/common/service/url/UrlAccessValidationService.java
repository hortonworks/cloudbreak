package com.sequenceiq.cloudbreak.common.service.url;

import javax.annotation.PostConstruct;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.cloudbreak.client.RestClientUtil;

@Service
public class UrlAccessValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UrlAccessValidationService.class);

    private Client client;

    @PostConstruct
    public void init() {
        // Needs to set to secure to make sure that the following Java bug is workarounded (since SNI is disabled in case of custom hostverifier):
        // https://bugs.openjdk.java.net/browse/JDK-8144566
        // https://en.wikipedia.org/wiki/Server_Name_Indication
        client = RestClientUtil.get(new ConfigKey(true, false, false));
    }

    public boolean isAccessible(String url) {
        LOGGER.debug("Validation of url access: {}", url);
        boolean result = false;
        try {
            WebTarget target = client.target(url);
            try (Response response = target.request().head()) {
                if (HttpStatus.OK.value() == response.getStatus()) {
                    result = true;
                }
            }
        } catch (ProcessingException ex) {
            LOGGER.info("The following URL is not reachable by Cloudbreak: '{}', reason: {}", url, ex.getMessage());
        }
        return result;
    }
}
