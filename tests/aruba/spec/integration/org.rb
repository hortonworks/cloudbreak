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

RSpec.describe 'Organization test cases', :type => :aruba, :feature => "Organizations", :severity => :critical do
  include_context "shared command helpers"
  include_context "mock shared vars"

  before(:each) do
    MockHelper.resetMock()
  end

  xit "Organization - List", :story => "List Organizations", :severity => :normal, :testId => 1 do
    with_environment 'DEBUG' => '1' do
      responseHash = MockHelper.getResponseHash("../../../responses/organizations/organizations.json")

      expectedEndpointResponse = TraceResponseBuilder.getOrganizationsResponseFactory(responseHash)
      MockHelper.setupResponse("getOrganizations", responseHash)

      result = cb.org.list.build(false)
      resultHash = MockHelper.getResultHash(result.output)

      expect(result.exit_status).to eql 0
      expect(result.stderr.to_s.downcase).not_to include("error")
      expect(MockHelper.getResponseDiff(expectedEndpointResponse, resultHash)).to be_truthy
    end
  end

  xit "Organization - Describe", :story => "Describe Organizations", :severity => :normal, :testId => 2 do
    with_environment 'DEBUG' => '1' do
      responseHash = MockHelper.getResponseHash("../../../responses/organizations/organizations.json")

      expectedEndpointResponse = TraceResponseBuilder.getOrganizationByNameResponseFactory(responseHash)
      MockHelper.setupResponse("getOrganizationByName", responseHash)

      result = cb.org.describe.name("mock@hortonworks.com").build(false)
      resultHash = MockHelper.getResultHash(result.output)

      #expect(result.exit_status).to eql 0
      expect(result.stderr.to_s.downcase).not_to include("error")
      expect(MockHelper.getResponseDiff(expectedEndpointResponse, resultHash)).to be_truthy
    end
  end

  xit "Organization - Create", :story => "Create Organizations", :severity => :critical, :testId => 3 do
    with_environment 'DEBUG' => '1' do
      requestHash = MockHelper.getResponseHash("../../../requests/organizations/organizations.json")

      expectedEndpointCall = TraceResponseBuilder.createOrganizationRequestFactory(requestHash)

      result = cb.org.create.name("integration-testing").description("test").build(false)

      expect(result.exit_status).to eql 0
      expect(result.stderr.to_s.downcase).not_to include("error")
      expect(MockHelper.getRequestDiff(expectedEndpointCall)).to be_truthy
    end
  end

  xit "Organization - Delete", :story => "Delete Organizations", :severity => :critical, :testId => 4 do
    with_environment 'DEBUG' => '1' do
      responseHash = MockHelper.getResponseHash("../../../responses/organizations/deleted-organization.json")

      expectedEndpointResponse = TraceResponseBuilder.deleteOrganizationByNameResponseFactory(responseHash)
      MockHelper.setupResponse("deleteOrganizationByName", responseHash)

      result = cb.org.delete.name("mock@hortonworks.com").build(false)
      resultHash = MockHelper.getResultHash(result.output)

      expect(result.exit_status).to eql 0
      expect(result.stderr.to_s.downcase).not_to include("error")
      expect(MockHelper.getResponseDiff(expectedEndpointResponse, resultHash)).to be_truthy
    end
  end
end