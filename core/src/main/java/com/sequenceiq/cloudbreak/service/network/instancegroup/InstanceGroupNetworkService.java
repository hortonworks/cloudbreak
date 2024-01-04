package com.sequenceiq.cloudbreak.service.network.instancegroup;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.repository.InstanceGroupNetworkRepository;

@Service
public class InstanceGroupNetworkService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceGroupNetworkService.class);

    @Inject
    private InstanceGroupNetworkRepository networkRepository;

    public InstanceGroupNetwork create(InstanceGroupNetwork network) {
        try {
            return networkRepository.save(network);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.INSTANCE_GROUP_NETWORK, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
    }

    public InstanceGroupNetwork get(Long id) {
        return networkRepository.findById(id).orElseThrow(notFound("InstanceGroupNetwork", id));
    }

    public InstanceGroupNetwork savePure(InstanceGroupNetwork instanceGroupNetwork) {
        return networkRepository.save(instanceGroupNetwork);
    }
}
