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

RSpec.describe 'Environment test cases', :type => :aruba, :feature => "Environments", :severity => :critical do
  include_context "shared command helpers"    
  include_context "mock shared vars"   

  before(:each) do
      MockHelper.resetMock("env")
  end

  it "Environment - List", :story => "Environments", :severity => :normal, :testId => 1 do
    with_environment 'DEBUG' => '1' do
      responseHash = MockHelper.getResponseHash("../../../responses/environments/environments.json")
      expectedEndpointResponse = TraceResponseBuilder.listEnvironmentsResponseFactory(responseHash)
      MockHelper.setupResponse("env", "listEnvironmentV1", responseHash)

      result = cb.env.list.build(true)
      resultHash = MockHelper.getResultHash(result.output)

      expect(result.exit_status).to eql 0
      #expect(result.stderr.to_s.downcase).to include("com.sequenceiq.environment.exception.FreeIpaOperationFailedException: FreeIpa deletion operation failed: Termination failed: The security token included in the request is invalid. (Service: AmazonCloudFormation; Status Code: 403; Error Code: InvalidClientTokenId; Request ID: 9f77a882-a3b8-11e9-a694-a5e2a1fba380)")
      expect(MockHelper.getResponseDiff(expectedEndpointResponse, resultHash)).to be_truthy
    end
  end

  it "Environment - Create AWS Environment", :story => "AWS Environments", :severity => :normal, :testId => 2 do
    with_environment 'DEBUG' => '1' do
     responseHash = MockHelper.getResponseHash("../../../responses/environments/post-aws-environment-response.json")
     requestHash = MockHelper.getResponseHash("../../../requests/environments/post-aws-environment-request.json")
     expectedEndpointRequest = TraceResponseBuilder.createEnvironmentRequestFactory(requestHash)
     MockHelper.setupResponse("env", "createEnvironmentV1", responseHash)

     result = cb.env.create.from_file.file(@environment_file).build(true)
     resultHash = MockHelper.getResultHash(result.output)

     expect(result.exit_status).to eql 0
     expect(result.stderr.to_s.downcase).not_to include("error")
     expect(MockHelper.getRequestDiff("env", expectedEndpointRequest)).to be_truthy
    end
  end
end