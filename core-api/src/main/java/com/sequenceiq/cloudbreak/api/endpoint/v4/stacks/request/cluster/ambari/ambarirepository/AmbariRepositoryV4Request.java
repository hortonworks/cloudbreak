package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.ambarirepository;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.repository.RepositoryV4Request;
import com.sequenceiq.cloudbreak.validation.ValidAmbariRepo;

import io.swagger.annotations.ApiModel;

@ValidAmbariRepo
@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AmbariRepositoryV4Request extends RepositoryV4Request {

}
