package com.sequenceiq.mock.spi.controller;

import static com.sequenceiq.mock.spi.controller.DbResourceController.CERTIFICATE_1;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.service.ResponseModifierService;
import com.sequenceiq.mock.spi.DbDto;
import com.sequenceiq.mock.spi.DbStoreService;
import com.sequenceiq.mock.verification.RequestResponseStorageService;

@RestController
@RequestMapping("/{mock_uuid}/db")
public class DbController {

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    @Inject
    private DbStoreService dbStoreService;

    @Inject
    private ResponseModifierService responseModifierService;

    @Inject
    private RequestResponseStorageService requestResponseStorageService;

    @PostMapping()
    public CloudResourceStatus[] launch(@PathVariable("mock_uuid") String mockuuid, @RequestBody DatabaseStack databaseStack) {
        List<CloudResourceStatus> cloudResourceStatuses = dbStoreService.store(mockuuid, databaseStack);
        return cloudResourceStatuses.toArray(new CloudResourceStatus[cloudResourceStatuses.size()]);
    }

    @DeleteMapping()
    public void terminate(@PathVariable("mock_uuid") String mockuuid) {
        dbStoreService.terminate(mockuuid);
    }

    @GetMapping()
    public ExternalDatabaseStatus get(@PathVariable("mock_uuid") String mockuuid) {
        DbDto db = dbStoreService.read(mockuuid);
        return db.getExternalDatabaseStatus();
    }

    @PutMapping()
    public void startStop(@PathVariable("mock_uuid") String mockuuid, @RequestBody Boolean startOrStop) {
        DbDto db = dbStoreService.read(mockuuid);
        db.setExternalDatabaseStatus(startOrStop ? ExternalDatabaseStatus.STARTED : ExternalDatabaseStatus.STOPPED);
    }

    @GetMapping("activecertificate")
    public String getActiveCertificate(@PathVariable("mock_uuid") String mockuuid) {
        return CERTIFICATE_1;
    }

    @PutMapping("upgrade")
    public void upgrade(@PathVariable("mock_uuid") String mockuuid, @RequestBody String targetMajorVersion) {
    }

    @PostMapping("upgrade")
    public void validateUpgrade(@PathVariable("mock_uuid") String mockuuid, @RequestBody String targetMajorVersion) {
    }
}
