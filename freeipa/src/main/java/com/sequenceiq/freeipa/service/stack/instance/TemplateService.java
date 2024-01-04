package com.sequenceiq.freeipa.service.stack.instance;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.repository.TemplateRepository;

@Service
public class TemplateService {

    @Inject
    private TemplateRepository repository;

    public Template save(Template template) {
        return repository.save(template);
    }

    public Optional<Template> findById(Long id) {
        return repository.findById(id);
    }

}
