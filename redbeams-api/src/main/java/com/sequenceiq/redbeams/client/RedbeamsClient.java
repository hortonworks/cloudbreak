package com.sequenceiq.redbeams.client;

import static com.sequenceiq.redbeams.api.RedbeamsApi.API_ROOT_CONTEXT;

import javax.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClient;
import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.redbeams.api.endpoint.v4.database.DatabaseV4Endpoint;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

public class RedbeamsClient extends AbstractUserCrnServiceClient {

    private final Logger logger = LoggerFactory.getLogger(RedbeamsClient.class);

    public RedbeamsClient(String redbeamsAddress, ConfigKey configKey) {
        super(redbeamsAddress, configKey, API_ROOT_CONTEXT);
        logger.info("RedbeamsClient has been created. redbeams: {}, configKey: {}", redbeamsAddress, configKey);
    }

    private ExpiringMap<String, String> configTokenCache() {
        return ExpiringMap.builder().variableExpiration().expirationPolicy(ExpirationPolicy.CREATED).build();
    }

    @Override
    public <T extends AbstractUserCrnServiceEndpoint> T withCrn(String crn) {
        return null;
    }

    public static class ReadbeamsEndpoint extends AbstractUserCrnServiceEndpoint {

        protected ReadbeamsEndpoint(WebTarget webTarget, String crn) {
            super(webTarget, crn);
        }

        public DatabaseV4Endpoint databaseEndpoint() {
            return getEndpoint(DatabaseV4Endpoint.class);
        }
    }
}
