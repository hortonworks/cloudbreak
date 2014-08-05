package com.sequenceiq.cloudbreak.service.company;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Company;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.repository.CompanyRepository;
import com.sequenceiq.cloudbreak.repository.UserRepository;

@Service
public class DefaultCompanyService implements CompanyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCompanyService.class);

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Company ensureCompany(String companyName) {
        LOGGER.debug("Checking the company: {}", companyName);
        Company company = companyRepository.findByName(companyName);
        if (null == company) {
            LOGGER.debug("Company with name <{}> not found. Creating it ....", companyName);
            company = new Company();
            company.setName(companyName);
            company = companyRepository.save(company);
            LOGGER.debug("Company registered,");
        }
        return company;
    }

    @Override
    public boolean companyExists(String companyName) {
        return null != companyRepository.findByName(companyName);
    }

    @Override
    public Set<User> companyUsers(Long companyId) {
        LOGGER.debug("Retrieving decorated users for company with id: [{}] ...", companyId);
        Set<User> users = companyRepository.companyUsers(companyId);
        LOGGER.debug("Found #{} users.", users.size());
        return users;
    }

    @Override
    public User companyAdmin(Long companyId) {
        LOGGER.debug("Retrieving company admin for company id: [{}] ...", companyId);
        User admin = companyRepository.findCompanyAdmin(companyId);
        if (admin == null) {
            throw new IllegalStateException("The company has no admin!");
        }
        LOGGER.debug("Found company admin; user id: [{}] .", admin.getId());
        return admin;
    }

    @Override
    public User companyUserData(Long companyId, UserRole role) {
        LOGGER.debug("Retrieving company user data for the company [{}] in userRole [{}] ...", companyId, role);
        User admin = companyRepository.findCompanyAdmin(companyId);
        User decoratedAdmin = userRepository.findOneWithLists(admin.getId());

        getAwsCredentialsForRole(decoratedAdmin, role);
        getAzureCredentialsForRole(decoratedAdmin, role);
        getAwsTemplatesForRole(decoratedAdmin, role);
        getAzureTemplatesForRole(decoratedAdmin, role);
        getSatcksForRole(decoratedAdmin, role);
        getBlueprintsForRole(decoratedAdmin, role);
        getClustersForRole(decoratedAdmin, role);

        return decoratedAdmin;
    }

    private void getClustersForRole(User decoratedAdmin, UserRole role) {
        Set<Cluster> clustersInRole = new HashSet<>();
        for (Cluster cluster : decoratedAdmin.getClusters()) {
            if (cluster.getUserRoles().contains(role)) {
                clustersInRole.add(cluster);
            }
        }
        decoratedAdmin.getClusters().clear();
        decoratedAdmin.getClusters().addAll(clustersInRole);
    }

    private void getBlueprintsForRole(User decoratedAdmin, UserRole role) {
        Set<Blueprint> blueprintsInRole = new HashSet<>();
        for (Blueprint blueprint : decoratedAdmin.getBlueprints()) {
            if (blueprint.getUserRoles().contains(role)) {
                blueprintsInRole.add(blueprint);
            }
        }
        decoratedAdmin.getBlueprints().clear();
        decoratedAdmin.getBlueprints().addAll(blueprintsInRole);
    }

    private void getSatcksForRole(User decoratedAdmin, UserRole role) {
        Set<Stack> stacksInRole = new HashSet<>();
        for (Stack stack : decoratedAdmin.getStacks()) {
            if (stack.getUserRoles().contains(role)) {
                stacksInRole.add(stack);
            }
        }
        decoratedAdmin.getStacks().clear();
        decoratedAdmin.getStacks().addAll(stacksInRole);
    }

    private void getAzureTemplatesForRole(User decoratedAdmin, UserRole role) {
        Set<AzureTemplate> azureTemplatesInRole = new HashSet<>();
        for (AzureTemplate azureTemplate : decoratedAdmin.getAzureTemplates()) {
            if (azureTemplate.getUserRoles().contains(role)) {
                azureTemplatesInRole.add(azureTemplate);
            }
        }
        decoratedAdmin.getAzureTemplates().clear();
        decoratedAdmin.getAzureTemplates().addAll(azureTemplatesInRole);
    }

    private void getAwsTemplatesForRole(User decoratedAdmin, UserRole role) {
        Set<AwsTemplate> awsTemplatesInRole = new HashSet<>();
        for (AwsTemplate awsTemplate : decoratedAdmin.getAwsTemplates()) {
            if (awsTemplate.getUserRoles().contains(role)) {
                awsTemplatesInRole.add(awsTemplate);
            }
        }
        decoratedAdmin.getAwsTemplates().clear();
        decoratedAdmin.getAwsTemplates().addAll(awsTemplatesInRole);
    }

    private void getAzureCredentialsForRole(User decoratedAdmin, UserRole role) {
        Set<AzureCredential> azureCredentialsInRole = new HashSet<>();
        for (AzureCredential azureCredential : decoratedAdmin.getAzureCredentials()) {
            if (azureCredential.getUserRoles().contains(role)) {
                azureCredentialsInRole.add(azureCredential);
            }
        }
        decoratedAdmin.getAzureCredentials().clear();
        decoratedAdmin.getAzureCredentials().addAll(azureCredentialsInRole);
    }

    private void getAwsCredentialsForRole(User decoratedAdmin, UserRole role) {
        Set<AwsCredential> awsCredentialsInRole = new HashSet<>();
        for (AwsCredential awsCredential : decoratedAdmin.getAwsCredentials()) {
            if (awsCredential.getUserRoles().contains(role)) {
                awsCredentialsInRole.add(awsCredential);
            }
        }
        decoratedAdmin.getAwsCredentials().clear();
        decoratedAdmin.getAwsCredentials().addAll(awsCredentialsInRole);
    }

}
