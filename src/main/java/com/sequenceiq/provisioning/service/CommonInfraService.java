package com.sequenceiq.provisioning.service;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.IteratorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.provisioning.controller.json.InfraRequest;
import com.sequenceiq.provisioning.converter.AwsInfraConverter;
import com.sequenceiq.provisioning.converter.AzureInfraConverter;
import com.sequenceiq.provisioning.domain.AwsInfra;
import com.sequenceiq.provisioning.domain.AzureInfra;
import com.sequenceiq.provisioning.repository.AwsInfraRepository;
import com.sequenceiq.provisioning.repository.AzureInfraRepository;

@Service
public class CommonInfraService {

    @Autowired
    private AwsInfraRepository awsInfraRepository;

    @Autowired
    private AzureInfraRepository azureInfraRepository;

    @Autowired
    private AwsInfraConverter awsInfraConverter;

    @Autowired
    private AzureInfraConverter azureInfraConverter;

    public Set<InfraRequest> getAll() {
        Set<InfraRequest> result = new HashSet<>();
        result.addAll(awsInfraConverter.convertAllEntityToJson(IteratorUtils.toList(awsInfraRepository.findAll().iterator())));
        result.addAll(azureInfraConverter.convertAllEntityToJson(IteratorUtils.toList(azureInfraRepository.findAll().iterator())));
        return result;
    }

    public InfraRequest get(Long id) {
        AwsInfra awsInfra = awsInfraRepository.findOne(id);
        if (awsInfra == null) {
            AzureInfra azureInfra = azureInfraRepository.findOne(id);
            if (azureInfra == null) {
                return azureInfraConverter.convert(azureInfra);
            } else {
                return null;
            }
        } else {
            return awsInfraConverter.convert(awsInfra);
        }
    }

}
