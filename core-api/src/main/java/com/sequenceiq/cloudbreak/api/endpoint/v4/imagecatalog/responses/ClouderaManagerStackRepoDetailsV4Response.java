package com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClouderaManagerStackRepoDetailsV4Response extends BaseStackRepoDetailsV4Response implements JsonEntity {

}
