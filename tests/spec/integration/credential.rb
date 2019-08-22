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

  it "Credential - List", :story => "Credentials", :severity => :normal, :testId => 1 do
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

  it "Credential - AWS Prerequisites AWS", :story => "AWS Credentials", :severity => :normal, :testId => 2 do
    with_environment 'DEBUG' => '1' do
      result = cb.credential.prerequisites.aws.build(false)

      expect(result.exit_status).to eql 0
      expect(result.stdout.empty?).to be_falsy
      [JSON.parse(result.stdout)].flatten.each do |s|
            expect(s).to include_json(
              accountId: /.*/,
              cloudPlatform: /.*/,
              externalId: /.*/,
              policyJSON: /.*/
          )
      end
    end
  end

  it "Credential - Create AWS Credential", :story => "AWS Credentials", :severity => :normal, :testId => 3 do
    with_environment 'DEBUG' => '1' do
      responseHash = MockHelper.getResponseHash("../../../responses/credentials/post-aws-credential-response.json")
      requestHash = MockHelper.getResponseHash("../../../requests/credentials/post-aws-credential-request.json")
      expectedEndpointRequest = TraceResponseBuilder.createCredentialRequestFactory(requestHash)
      MockHelper.setupResponse("env", "createCredentialV1", responseHash)

      result = cb.credential.create.aws.key_based.name("cli-aws-key").access_key(@aws_access_key).secret_key(@aws_secret_key).build(false)
      resultHash = MockHelper.getResultHash(result.output)

      expect(result.exit_status).to eql 0
      expect(result.stderr.to_s.downcase).not_to include("error")
      expect(MockHelper.getRequestDiff("env", expectedEndpointRequest)).to be_truthy
    end
  end

  it "Credential - Describe AWS Credential", :story => "AWS Credentials", :severity => :normal, :testId => 4 do
    with_environment 'DEBUG' => '1' do
      responseHash = MockHelper.getResponseHash("../../../responses/credentials/post-aws-credential-response.json")
      expectedEndpointResponse = TraceResponseBuilder.getCredentialByNameV1ResponseFactory(responseHash)
      MockHelper.setupResponse("env", "getCredentialByNameV1", responseHash)

      result = cb.credential.describe.name("cli-aws-key").build(false)
      resultHash = MockHelper.getResultHash(result.output)

      expect(result.exit_status).to eql 0
      expect(result.stderr.to_s.downcase).not_to include("error")
      expect(MockHelper.getResponseDiff(expectedEndpointResponse, resultHash)).to be_truthy
    end
  end

  it "Credential - Modify AWS Credential", :story => "AWS Credentials", :severity => :normal, :testId => 5 do
    with_environment 'DEBUG' => '1' do
      getResponseHash = MockHelper.getResponseHash("../../../responses/credentials/put-aws-credential-response.json")
      getExpectedEndpointResponse = TraceResponseBuilder.getCredentialByNameV1ResponseFactory(getResponseHash)
      MockHelper.setupResponse("env", "getCredentialByNameV1", getResponseHash)

      putResponseHash = MockHelper.getResponseHash("../../../responses/credentials/put-aws-credential-response.json")
      putRequestHash = MockHelper.getResponseHash("../../../requests/credentials/put-aws-credential-request.json")
      putExpectedEndpointRequest = TraceResponseBuilder.modifyCredentialRequestFactory(putRequestHash)
      MockHelper.setupResponse("env", "putCredentialV1", putResponseHash)

      result = cb.credential.modify.aws.key_based.name("amazon").access_key(@aws_access_key).secret_key(@aws_secret_key).build(false)
      resultHash = MockHelper.getResultHash(result.output)

      expect(result.exit_status).to eql 0
      expect(result.stderr.to_s.downcase).not_to include("error")
      expect(MockHelper.getRequestDiff("env", putExpectedEndpointRequest)).to be_truthy
    end
  end

  it "Credential - Delete AWS Credential", :story => "AWS Credentials", :severity => :normal, :testId => 6 do
    with_environment 'DEBUG' => '1' do
      responseHash = MockHelper.getResponseHash("../../../responses/credentials/delete-aws-credential-response.json")
      expectedEndpointResponse = TraceResponseBuilder.listCredentialsResponseFactory(responseHash)
      MockHelper.setupResponse("env", "deleteCredentialsV1", responseHash)

      result = cb.credential.delete.names("amazon").build(false)
      resultHash = MockHelper.getResultHash(result.output)

      expect(result.exit_status).to eql 0
      expect(result.stderr.to_s.downcase).not_to include("error")
      expect(MockHelper.getResponseDiff(expectedEndpointResponse, resultHash)).to be_truthy
    end
  end
end