package com.sequenceiq.cloudbreak.converter;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.controller.json.StackJson;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;

@Component
public class StackConverter extends AbstractConverter<StackJson, Stack> {

    public static final int HASH1 = 0xFF;
    public static final int HASH2 = 0x100;
    public static final int END_INDEX = 3;
    public static final int BEGIN_INDEX = 1;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private ClusterConverter clusterConverter;

    @Override
    public StackJson convert(Stack entity) {
        StackJson stackJson = new StackJson();
        stackJson.setTemplateId(entity.getTemplate().getId());
        stackJson.setNodeCount(entity.getNodeCount());
        stackJson.setName(entity.getName());
        stackJson.setId(entity.getId());
        stackJson.setCloudPlatform(entity.getTemplate().cloudPlatform());
        stackJson.setCredentialId(entity.getCredential().getId());
        stackJson.setStatus(entity.getStatus());
        stackJson.setAmbariServerIp(entity.getAmbariIp());
        stackJson.setHash(entity.getHash());
        if (entity.getCluster() != null) {
            stackJson.setCluster(clusterConverter.convert(entity.getCluster(), "{}"));
        } else {
            stackJson.setCluster(new ClusterResponse());
        }
        return stackJson;
    }

    public StackJson convert(Stack entity, StackDescription description) {
        StackJson stackJson = new StackJson();
        stackJson.setTemplateId(entity.getTemplate().getId());
        stackJson.setNodeCount(entity.getNodeCount());
        stackJson.setId(entity.getId());
        stackJson.setName(entity.getName());
        stackJson.setCredentialId(entity.getCredential().getId());
        stackJson.setCloudPlatform(entity.getTemplate().cloudPlatform());
        stackJson.setDescription(description);
        stackJson.setStatus(entity.getStatus());
        stackJson.setHash(entity.getHash());
        stackJson.setAmbariServerIp(entity.getAmbariIp());
        if (entity.getCluster() != null) {
            stackJson.setCluster(clusterConverter.convert(entity.getCluster(), "{}"));
        } else {
            stackJson.setCluster(new ClusterResponse());
        }
        return stackJson;
    }

    @Override
    public Stack convert(StackJson json) {
        Stack stack = new Stack();
        stack.setNodeCount(json.getNodeCount());
        stack.setName(json.getName());
        try {
            stack.setCredential(credentialRepository.findOne(json.getCredentialId()));
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(String.format("Access to credential '%s' is denied or credential doesn't exist.", json.getCredentialId()), e);
        }
        try {
            stack.setTemplate(templateRepository.findOne(Long.valueOf(json.getTemplateId())));
        } catch (AccessDeniedException e) {
            throw new AccessDeniedException(String.format("Access to template '%s' is denied or template doesn't exist.", json.getTemplateId()), e);
        }
        stack.setStatus(Status.REQUESTED);
        return stack;

    }

    public String getMD5(Stack stack) {
        try {
            int hashCode = HashCodeBuilder.reflectionHashCode(stack);
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(String.valueOf(hashCode).getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & HASH1) | HASH2).substring(BEGIN_INDEX, END_INDEX));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }
}
