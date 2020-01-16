package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import com.sequenceiq.it.cloudbreak.dto.mock.SparkUri;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.AnswerWithoutRequest;

public final class ImageCatalogEndpoint {
    public static final String IMAGE_CATALOG = "/imagecatalog";

    private ImageCatalogEndpoint() {
    }

    @SparkUri(url = IMAGE_CATALOG + "/imagecatalog")
    public interface Base {
        AnswerWithoutRequest<String> getCatalog();

        AnswerWithoutRequest<String> head();
    }
}
