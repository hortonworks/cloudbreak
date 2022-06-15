package com.sequenceiq.consumption.converter.metering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.metering.events.MeteringEventsProto;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.dto.StorageConsumptionResult;
import com.sequenceiq.consumption.util.CloudStorageLocationUtil;

@Component
public class ConsumptionToStorageHeartbeatConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionToStorageHeartbeatConverter.class);

    private static final long NO_BYTE_IN_MB = 1000L * 1000L;

    public MeteringEventsProto.StorageHeartbeat convertToS3StorageHeartBeat(Consumption consumption, StorageConsumptionResult storage) {
        MeteringEventsProto.StorageHeartbeat.Builder storageHeartbeatBuilder = MeteringEventsProto.StorageHeartbeat.newBuilder();

        MeteringEventsProto.Storage.Builder storageBuilder = MeteringEventsProto.Storage.newBuilder();
        storageBuilder.setId(CloudStorageLocationUtil.getS3BucketName(consumption.getStorageLocation()));
        storageBuilder.setSizeInMB(storage.getStorageInBytes() / NO_BYTE_IN_MB);
        storageBuilder.setType(MeteringEventsProto.StorageType.Value.S3);
        storageHeartbeatBuilder.addStorages(storageBuilder.build());

        MeteringEventsProto.MeteredResourceMetadata.Builder metaBuilder = MeteringEventsProto.MeteredResourceMetadata.newBuilder();
        metaBuilder.setEnvironmentCrn(consumption.getEnvironmentCrn());
        storageHeartbeatBuilder.setMeteredResourceMetadata(metaBuilder.build());

        MeteringEventsProto.StorageHeartbeat ret = storageHeartbeatBuilder.build();
        LOGGER.debug("Converted StorageHeartbeat event: {}", ret);
        return ret;
    }
}
