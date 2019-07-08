require_relative "../common/mock_vars"
require_relative "../common/command_helpers"
require_relative "../common/mock_helper"
require_relative "../common/trace_response_builder"
require_relative "../common/response_helpers"
require_relative "spec_helper"



define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "dp "
  return cb
end

RSpec.describe 'Credential test cases', :type => :aruba, :feature => "Credentials", :severity => :critical do
  include_context "shared command helpers"
  include_context "mock shared vars"

  before(:each) do
      MockHelper.resetMock("env")
  end

  it "Credential - List", :story => "List Credentials", :severity => :normal, :testId => 1 do
    with_environment 'DEBUG' => '1' do
      responseHash = MockHelper.getResponseHash("../../../responses/credentials/credentials.json")

      expectedEndpointResponse = TraceResponseBuilder.listCredentialsResponseFactory(responseHash)
      MockHelper.setupResponse("env", "listCredentialsV1", responseHash)

      result = cb.credential.list.build(false)
      resultHash = MockHelper.getResultHash(result.output)

      expect(result.exit_status).to eql 0
      expect(result.stderr.to_s.downcase).not_to include("error")
      expect(MockHelper.getResponseDiff(expectedEndpointResponse, resultHash)).to be_truthy
    end
  end

  it "Credential - Describe AWS Credential", :story => "Describe AWS Credential", :severity => :normal, :testId => 2 do
    with_environment 'DEBUG' => '1' do
      responseHash = MockHelper.getResponseHash("../../../responses/credentials/create-aws.json")

      expectedEndpointResponse = TraceResponseBuilder.getCredentialByNameV1ResponseFactory(responseHash)
      MockHelper.setupResponse("env", "getCredentialByNameV1", responseHash)

      result = cb.credential.describe.name("cli-aws-key").build(false)
      resultHash = MockHelper.getResultHash(result.output)

      expect(result.exit_status).to eql 0
      expect(result.stderr.to_s.downcase).not_to include("error")
      expect(MockHelper.getResponseDiff(expectedEndpointResponse, resultHash)).to be_truthy
    end
  end
end