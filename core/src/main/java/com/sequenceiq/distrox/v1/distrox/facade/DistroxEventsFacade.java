package com.sequenceiq.distrox.v1.distrox.facade;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.distrox.api.v1.distrox.model.event.DistroXEventV1Response;

public interface DistroxEventsFacade {

    List<DistroXEventV1Response> retrieveEvents(Long since);

    Page<DistroXEventV1Response> retrieveEventsByStack(String stackCrn, Pageable pageable);

}
