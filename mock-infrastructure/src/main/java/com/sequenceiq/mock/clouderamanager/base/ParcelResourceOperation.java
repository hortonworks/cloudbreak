package com.sequenceiq.mock.clouderamanager.base;

import static com.sequenceiq.mock.clouderamanager.CommandId.START_PARCEL_DOWNLOAD;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.clouderamanager.CommandId;
import com.sequenceiq.mock.clouderamanager.DataProviderService;
import com.sequenceiq.mock.clouderamanager.ResponseCreatorComponent;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiParcel;
import com.sequenceiq.mock.swagger.model.ApiProductVersion;

@Service
public class ParcelResourceOperation {

    private static final Logger LOGGER = getLogger(ParcelResourceOperation.class);

    @Inject
    private ResponseCreatorComponent responseCreatorComponent;

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    @Inject
    private DataProviderService dataProviderService;

    public ResponseEntity<ApiCommand> startDownloadCommand(String mockUuid, String clusterName, String product, String version) {
        clouderaManagerStoreService.addOrUpdateProduct(mockUuid, product, version);
        return responseCreatorComponent.exec(dataProviderService.getSuccessfulApiCommand(START_PARCEL_DOWNLOAD));
    }

    public ResponseEntity<ApiParcel> readParcel(String mockUuid, String clusterName, String product, String version) {
        Optional<ApiProductVersion> clouderaManagerProduct = clouderaManagerStoreService.getClouderaManagerProduct(mockUuid, product);
        ApiParcel apiParcel = null;
        if (clouderaManagerProduct.isPresent()) {
            ApiProductVersion apiProductVersion = clouderaManagerProduct.get();
            apiParcel = new ApiParcel()
                    .product(apiProductVersion.getProduct())
                    .version(apiProductVersion.getVersion())
                    .stage("ACTIVATED");
        } else {
            LOGGER.info("Cannot find parcel for product: {}", product);
        }
        return responseCreatorComponent.exec(apiParcel);
    }

    public ResponseEntity<ApiCommand> startDistributionCommand(String mockUuid, String clusterName, String product, String version) {
        clouderaManagerStoreService.addOrUpdateProduct(mockUuid, product, version);
        return responseCreatorComponent.exec(dataProviderService.getSuccessfulApiCommand(CommandId.START_DISTRIBUTION_PARCEL));
    }

    public ResponseEntity<ApiCommand> activateCommand(String mockUuid, String clusterName, String product, String version) {
        clouderaManagerStoreService.addOrUpdateProduct(mockUuid, product, version);
        return responseCreatorComponent.exec(dataProviderService.getSuccessfulApiCommand(CommandId.ACTIVATE_PARCEL));
    }
}
