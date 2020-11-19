package com.sequenceiq.mock.clouderamanager;

import javax.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ProfileAwareComponent {

    @Inject
    private DefaultModelService defaultModelService;

    public <T> ResponseEntity<Void> exec() {
        return ProfileAwareResponse.get((Void) null, defaultModelService).handle();
    }

    public <T> ResponseEntity<T> exec(T apiAuthRoleMetadataList) {
        return ProfileAwareResponse.exec(apiAuthRoleMetadataList, defaultModelService);
    }
}
