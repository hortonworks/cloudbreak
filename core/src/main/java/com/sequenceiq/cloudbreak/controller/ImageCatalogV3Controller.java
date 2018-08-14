package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.ImageCatalogV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogRequest;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.ImageCatalogResponse;

@Controller
@Transactional(TxType.NEVER)
public class ImageCatalogV3Controller extends NotificationController implements ImageCatalogV3Endpoint {

    @Override
    public Set<ImageCatalogResponse> listByOrganization(Long organizationId) {
        return null;
    }

    @Override
    public ImageCatalogResponse getByNameInOrganization(Long organizationId, String name) {
        return null;
    }

    @Override
    public ImageCatalogResponse createInOrganization(Long organizationId, ImageCatalogRequest request) {
        return null;
    }

    @Override
    public ImageCatalogResponse deleteInOrganization(Long organizationId, String name) {
        return null;
    }
}
