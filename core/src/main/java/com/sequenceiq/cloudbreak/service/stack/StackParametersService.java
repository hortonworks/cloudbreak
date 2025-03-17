package com.sequenceiq.cloudbreak.service.stack;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.StackParameters;
import com.sequenceiq.cloudbreak.repository.StackParametersRepository;

@Service
public class StackParametersService {

    @Inject
    private StackParametersRepository stackParametersRepository;

    public List<StackParameters> findAllByStackId(Long stackId) {
        return stackParametersRepository.findAllByStackId(stackId);
    }

    public Optional<StackParameters> findStackParametersByStackIdAndKey(Long stackId, String key) {
        return stackParametersRepository.findStackParametersByStackIdAndKey(stackId, key);
    }

    public StackParameters save(StackParameters stackParameters) {
        return stackParametersRepository.save(stackParameters);
    }

    public void setStackParameter(Long stackId, String key, String value) {
        StackParameters stackParameters = findStackParametersByStackIdAndKey(stackId, key).orElse(new StackParameters());
        stackParameters.setStackId(stackId);
        stackParameters.setKey(key);
        stackParameters.setValue(value);
        save(stackParameters);
    }
}
