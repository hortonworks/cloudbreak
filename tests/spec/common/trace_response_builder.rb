require 'rest-client'
require 'json'
require_relative "../integration/spec_helper"

class TraceResponseBuilder
    @@default_workspace = 1

    @@cloudbreak_base = "cb"
    @@api_base = "#{@@cloudbreak_base}/api"
    @@clusterdefinition_base = "#{@@api_base}/v4/#{@@default_workspace}/clusterdefinitions"
    @@create_clusterdefinition_endpoint = "#{@@clusterdefinition_base}"
    @@cluster_base = "#{@@api_base}/v4/#{@@default_workspace}/stack"
    @@create_cluster_endpoint = "#{@@cluster_base}"
    @@create_workspace_endpoint = "#{@@api_base}/v4/workspaces"
    @@get_workspace_endpoint = "#{@@api_base}/v4/workspaces"
    @@get_users_endpoint = "#{@@api_base}/v4/users"

    def self.createWorkspaceRequestFactory(requestBody)
        return {
            :calledEndpoint => @@create_workspace_endpoint,
            :sentValue => requestBody
        }
    end

    def self.getWorkspacesResponseFactory(responseBody)
        return {
            :calledEndpoint => @@get_workspace_endpoint,
            :receivedValue => responseBody
        }
    end

    def self.getWorkspaceByNameResponseFactory(responseBody)
        return {
            :calledEndpoint => "#{@@get_workspace_endpoint}/name/mock@hortonworks.com",
            :receivedValue => responseBody
        }
    end

    def self.deleteWorkspaceByNameResponseFactory(responseBody)
        return {
            :calledEndpoint => "#{@@get_workspace_endpoint}/name/mock@hortonworks.com",
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
