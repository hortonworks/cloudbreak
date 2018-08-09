require 'rest-client'
require 'json'
require_relative "../integration/spec_helper"

class TraceResponseBuilder
    @@cloudbreak_base = "cb"
    @@api_base = "#{@@cloudbreak_base}/api"
    @@blueprint_base = "#{@@api_base}/v1/blueprints"
    @@create_blueprint_endpoint = "#{@@blueprint_base}/user"
    @@cluster_base = "#{@@api_base}/v2/stacks"
    @@create_cluster_endpoint = "#{@@cluster_base}/user"
    @@create_organization_endpoint = "#{@@api_base}/v1/organizations"
    @@get_organization_endpoint = "#{@@api_base}/v1/organizations"
    @@get_users_endpoint = "#{@@api_base}/v1/users"

    def self.createOrganizationRequestFactory(requestBody)
        return {
            :calledEndpoint => @@create_organization_endpoint,
            :sentValue => requestBody
        }
    end

    def self.getOrganizationsResponseFactory(responseBody)
        return {
            :calledEndpoint => @@get_organization_endpoint,
            :receivedValue => responseBody
        }
    end

    def self.getOrganizationByNameResponseFactory(responseBody)
        return {
            :calledEndpoint => "#{@@get_organization_endpoint}/name/mock@hortonworks.com",
            :receivedValue => responseBody
        }
    end

    def self.deleteOrganizationByNameResponseFactory(responseBody)
        return {
            :calledEndpoint => "#{@@get_organization_endpoint}/name/mock@hortonworks.com",
            :receivedValue => responseBody
        }
    end

    def self.getAllUsersResponseFactory(responseBody)
        return {
            :calledEndpoint => @@get_users_endpoint,
            :receivedValue => responseBody
        }
    end
end