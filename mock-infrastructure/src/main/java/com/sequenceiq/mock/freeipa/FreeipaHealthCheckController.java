package com.sequenceiq.mock.freeipa;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.freeipa.client.healthcheckmodel.CheckResult;
import com.sequenceiq.mock.spi.SpiDto;
import com.sequenceiq.mock.spi.SpiStoreService;

@RestController
@RequestMapping("/{mock_uuid}")
public class FreeipaHealthCheckController {

    @Inject
    private SpiStoreService spiStoreService;

    @RequestMapping("/freeipahealthcheck")
    public ResponseEntity<CheckResult> healthCheck(@PathVariable("mock_uuid") String mockUuid) {
        SpiDto read = spiStoreService.read(mockUuid.replace(":9443", ""));
        Optional<CloudVmMetaDataStatus> first = read.getVmMetaDataStatuses().stream().findFirst();
        CheckResult checkResult = new CheckResult();
        if (first.isPresent() && first.get().getCloudVmInstanceStatus().getStatus() == InstanceStatus.STARTED) {
            checkResult.setHost(first.get().getMetaData().getPublicIp());
            checkResult.setStatus("healthy");
            return ResponseEntity.ok(checkResult);
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }
}
