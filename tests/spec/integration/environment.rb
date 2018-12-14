require_relative "../common/mock_vars"
require_relative "../common/command_helpers"
require_relative "../common/response_helpers"
require_relative "spec_helper"


define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "dp "
  return cb
end

RSpec.describe 'Environment test cases', :type => :aruba do
  include_context "shared command helpers"    
  include_context "mock shared vars"   

  after(:all) do
    MockResponse.reset(ENV['BASE_URL'] + @mock_endpoint_reset)
  end

  it "Environment - List" do
    with_environment 'DEBUG' => '1' do
      result = cb.env.list.build(false)
      expect(result.exit_status).to eql 0
      expect(result.stdout.empty?).to be_falsy 
      JSON.parse(result.stdout).each do |s|
      expect(s).to include_json(
        Name: /.*/
      )
    end     
    end
  end
  
  it "Environment - Create" do
    with_environment 'DEBUG' => '1' do
      requestBody = MockResponse.requestBodyCreate('createEnvironment', load_json(@env_single_json), '200')
      url = ENV['BASE_URL'] + @mock_endpoint_setup
      MockResponse.post(requestBody, url)
      result = cb.env.create.name("test-env").credential("test-cred").regions("test").location_name("testlocation").build(false)
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("environment created")
    end
  end

  it "Environment - Describe" do
    with_environment 'DEBUG' => '1' do
      requestBody = MockResponse.requestBodyCreate('getEnvironment', load_json(@env_single_json), '200')
      url = ENV['BASE_URL'] + @mock_endpoint_setup
      MockResponse.post(requestBody, url)
      result = cb.env.describe.name("test-env").build(false)
      expect(result.exit_status).to eql 0 
      expect(JSON.parse(result.stdout)).to include_json(
        Name: /.*/
      )
    end
  end

  it "Environment - Delete" do
    with_environment 'DEBUG' => '1' do
      result = cb.env.delete.name("test-env").build(false)
      expect(result.exit_status).to eql 0
    end
  end  

  it "Environment - Attach ldap" do
    with_environment 'DEBUG' => '1' do
      requestBody = MockResponse.requestBodyCreate('attachResourcesToEnvironment', load_json(@env_single_json), '200')
      url = ENV['BASE_URL'] + @mock_endpoint_setup
      MockResponse.post(requestBody, url)
      result = cb.env.attach.name("test-env").ldap_names("test").build(false)
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("resources attached to environment") 
    end  
  end

  it "Environment - Attach proxy" do
    with_environment 'DEBUG' => '1' do
      requestBody = MockResponse.requestBodyCreate('attachResourcesToEnvironment', load_json(@env_single_json), '200')
      url = ENV['BASE_URL'] + @mock_endpoint_setup
      MockResponse.post(requestBody, url)
      result = cb.env.attach.name("test-env").proxy_names("test").build(false)
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("resources attached to environment") 
    end  
  end

  it "Environment - Attach rds" do
    with_environment 'DEBUG' => '1' do
      requestBody = MockResponse.requestBodyCreate('attachResourcesToEnvironment', load_json(@env_single_json), '200')
      url = ENV['BASE_URL'] + @mock_endpoint_setup
      MockResponse.post(requestBody, url)
      result = cb.env.attach.name("test-env").rds_names("test").build(false)
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("resources attached to environment") 
    end  
  end

  it "Environment - Attach ldap, proxy, rds" do
    with_environment 'DEBUG' => '1' do
      requestBody = MockResponse.requestBodyCreate('attachResourcesToEnvironment', load_json(@env_single_json), '200')
      url = ENV['BASE_URL'] + @mock_endpoint_setup
      MockResponse.post(requestBody, url)
      result = cb.env.attach.name("test-env").ldap_names("test").proxy_names("test").rds_names("test").build(false)
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("resources attached to environment") 
    end  
  end

  it "Environment - Detach ldap" do
    with_environment 'DEBUG' => '1' do
      requestBody = MockResponse.requestBodyCreate('detachResourcesToEnvironment', load_json(@env_single_json), '200')
      url = ENV['BASE_URL'] + @mock_endpoint_setup
      MockResponse.post(requestBody, url)
      result = cb.env.detach.name("test-env").ldap_names("test").build(false)
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("resources detached to environment") 
    end  
  end

  it "Environment - Detach proxy" do
    with_environment 'DEBUG' => '1' do
      requestBody = MockResponse.requestBodyCreate('detachResourcesToEnvironment', load_json(@env_single_json), '200')
      url = ENV['BASE_URL'] + @mock_endpoint_setup
      MockResponse.post(requestBody, url)
      result = cb.env.detach.name("test-env").proxy_names("test").build(false)
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("resources detached to environment") 
    end  
  end

  it "Environment - Detach rds" do
    with_environment 'DEBUG' => '1' do
      requestBody = MockResponse.requestBodyCreate('detachResourcesToEnvironment', load_json(@env_single_json), '200')
      url = ENV['BASE_URL'] + @mock_endpoint_setup
      MockResponse.post(requestBody, url)
      result = cb.env.detach.name("test-env").rds_names("test").build(false)
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("resources detached to environment") 
    end  
  end

  it "Environment - Detach ldap, proxy, rds" do
    with_environment 'DEBUG' => '1' do
      requestBody = MockResponse.requestBodyCreate('detachResourcesToEnvironment', load_json(@env_single_json), '200')
      url = ENV['BASE_URL'] + @mock_endpoint_setup
      MockResponse.post(requestBody, url)
      result = cb.env.detach.name("test-env").ldap_names("test").proxy_names("test").rds_names("test").build(false)
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("resources detached to environment") 
    end  
  end 

  it "Environment - Change credential" do
    with_environment 'DEBUG' => '1' do
      requestBody = MockResponse.requestBodyCreate('changeCredentialInEnvironment', load_json(@env_single_json), '200')
      url = ENV['BASE_URL'] + @mock_endpoint_setup
      MockResponse.post(requestBody, url)
      result = cb.env.change_cred.name("test-env").credential("test").build(false)
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("credential of environment test1 changed to") 
    end
  end                 
end  