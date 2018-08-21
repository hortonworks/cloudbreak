package com.sequenceiq.cloudbreak.service.image;

import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.service.organization.LegacyOrganizationAwareResourceService;

public interface LegacyImageCatalogService extends LegacyOrganizationAwareResourceService<ImageCatalog>, ImageCatalogService {
}
