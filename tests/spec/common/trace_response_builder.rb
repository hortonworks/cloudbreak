require 'rest-client'
require 'json'
require_relative "../integration/spec_helper"

class TraceResponseBuilder
    @@default_workspace = 1

    @@cb_base = "cb"
    @@dl_base = "dl"
    @@env_base = "environmentservice"
    @@cb_api_base = "#{@@cb_base}/api"
    @@dl_api_base = "#{@@dl_base}/api"
    @@env_api_base = "#{@@env_base}/api"
    @@blueprint_base = "#{@@cb_api_base}/v4/#{@@default_workspace}/blueprints"
    @@create_blueprint_endpoint = "#{@@blueprint_base}"
    @@cluster_base = "#{@@cb_api_base}/v4/#{@@default_workspace}/stack"
    @@create_cluster_endpoint = "#{@@cluster_base}"
    @@create_workspace_endpoint = "#{@@cb_api_base}/v4/workspaces"
    @@get_workspace_endpoint = "#{@@cb_api_base}/v4/workspaces"
    @@get_users_endpoint = "#{@@cb_api_base}/v4/users"
    @@list_credential_endpoint = "#{@@env_api_base}/v1/credentials"

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

    def self.listCredentialsResponseFactory(responseBody)
        return {
            :calledEndpoint => @@list_credential_endpoint,
            :receivedValue => responseBody
        }
    end

    def self.getCredentialByNameV1ResponseFactory(responseBody)
        return {
            :calledEndpoint => "#{@@list_credential_endpoint}/name/cli-aws-key",
            :receivedValue => responseBody
        }
    end
end
