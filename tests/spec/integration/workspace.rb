require_relative "../common/mock_vars"
require_relative "../common/command_helpers"
require_relative "../common/mock_helper"
require_relative "../common/trace_response_builder"
require_relative "spec_helper"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "cb "
  return cb
end

RSpec.describe 'Workspace test cases', :type => :aruba, :feature => "Workspaces", :severity => :critical do
  include_context "shared command helpers"
  include_context "mock shared vars"

  before(:each) do
    MockHelper.resetMock()
  end

  it "Workspace - List", :story => "List Workspaces", :severity => :normal, :testId => 1 do
    with_environment 'DEBUG' => '1' do
      responseHash = MockHelper.getResponseHash("../../../responses/workspaces/workspaces.json")

      expectedEndpointResponse = TraceResponseBuilder.getWorkspacesResponseFactory(responseHash)
      MockHelper.setupResponse("getWorkspaces", responseHash)

      result = cb.workspace.list.build(false)
      resultHash = MockHelper.getResultHash(result.output)

      expect(result.exit_status).to eql 0
      expect(result.stderr.to_s.downcase).not_to include("error")
      expect(MockHelper.getResponseDiff(expectedEndpointResponse, resultHash)).to be_truthy
    end
  end

  it "Workspace - Describe", :story => "Describe Workspaces", :severity => :normal, :testId => 2 do
    with_environment 'DEBUG' => '1' do
      responseHash = MockHelper.getResponseHash("../../../responses/workspaces/workspace.json")

      expectedEndpointResponse = TraceResponseBuilder.getWorkspaceByNameResponseFactory(responseHash)
      MockHelper.setupResponse("getWorkspaceByName", responseHash)

      result = cb.workspace.describe.name("mock@hortonworks.com").build(false)
      resultHash = MockHelper.getResultHash(result.output)

      #expect(result.exit_status).to eql 0
      expect(result.stderr.to_s.downcase).not_to include("error")
      expect(MockHelper.getResponseDiff(expectedEndpointResponse, resultHash)).to be_truthy
    end
  end

  it "Workspace - Create", :story => "Create Workspaces", :severity => :critical, :testId => 3 do
    with_environment 'DEBUG' => '1' do
      requestHash = MockHelper.getResponseHash("../../../requests/workspaces/workspaces.json")

      expectedEndpointCall = TraceResponseBuilder.createWorkspaceRequestFactory(requestHash)

      result = cb.workspace.create.name("integration-testing").description("test").build(false)

      expect(result.exit_status).to eql 0
      expect(result.stderr.to_s.downcase).not_to include("error")
      expect(MockHelper.getRequestDiff(expectedEndpointCall)).to be_truthy
    end
  end

  it "Workspace - Delete", :story => "Delete Workspaces", :severity => :critical, :testId => 4 do
    with_environment 'DEBUG' => '1' do
      responseHash = MockHelper.getResponseHash("../../../responses/workspaces/deleted-workspace.json")

      expectedEndpointResponse = TraceResponseBuilder.deleteWorkspaceByNameResponseFactory(responseHash)
      MockHelper.setupResponse("deleteWorkspaceByName", responseHash)

      result = cb.workspace.delete.name("mock@hortonworks.com").build(false)
      resultHash = MockHelper.getResultHash(result.output)

      expect(result.exit_status).to eql 0
      expect(result.stderr.to_s.downcase).not_to include("error")
      expect(MockHelper.getResponseDiff(expectedEndpointResponse, resultHash)).to be_truthy
    end
  end
end
