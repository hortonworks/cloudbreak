package com.sequenceiq.freeipa.service.stack;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.StackParameter;
import com.sequenceiq.freeipa.repository.StackParameterRepository;

@Service
public class StackParameterService {
    @Inject
    private StackParameterRepository stackParameterRepository;

    public List<StackParameter> findAllByStackId(Long stackId) {
        return stackParameterRepository.findAllByStackId(stackId);
    }

    public Optional<StackParameter> findStackParameterByStackIdAndParamKey(Long stackId, String paramKey) {
        return stackParameterRepository.findStackParameterByStackIdAndParamKey(stackId, paramKey);
    }

    public StackParameter save(StackParameter stackParameter) {
        return stackParameterRepository.save(stackParameter);
    }

    public void setStackParameter(Long stackId, String paramKey, String paramValue) {
        StackParameter stackParameter = findStackParameterByStackIdAndParamKey(stackId, paramKey).orElse(new StackParameter());
        stackParameter.setStackId(stackId);
        stackParameter.setParamKey(paramKey);
        stackParameter.setParamValue(paramValue);
        save(stackParameter);
    }
}
