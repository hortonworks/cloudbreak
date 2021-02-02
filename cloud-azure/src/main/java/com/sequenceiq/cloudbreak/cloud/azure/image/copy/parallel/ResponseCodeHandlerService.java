package com.sequenceiq.cloudbreak.cloud.azure.image.copy.parallel;

import org.springframework.stereotype.Service;

@Service
public class ResponseCodeHandlerService {

    public void handleResponse(int responseCode) {
        if (responseCode >= 400) {
            throw new RuntimeException("Blob copy return code is error: " + responseCode);
        }
    }

}
