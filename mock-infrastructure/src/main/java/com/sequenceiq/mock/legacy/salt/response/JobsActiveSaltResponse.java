package com.sequenceiq.mock.legacy.salt.response;

import static com.sequenceiq.mock.legacy.service.HostNameUtil.responseFromJsonFile;

import org.springframework.stereotype.Component;

import com.sequenceiq.mock.legacy.salt.SaltResponse;

@Component
public class JobsActiveSaltResponse implements SaltResponse {

    @Override
    public Object run(String body) throws Exception {
        return responseFromJsonFile("saltapi/runningjobs_response.json");
    }

    @Override
    public String cmd() {
        return "jobs.active";
    }
}
