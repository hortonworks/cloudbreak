require "common/e2e_vars.rb"
require "common/mock_vars.rb"
require "common/helpers.rb"
require "common/command_helpers.rb"
require "integration/spec_helper"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "cb "
  return cb
end

RSpec.describe 'Credential test cases', :type => :aruba do
  include_context "shared helpers"
  include_context "shared command helpers"    
  include_context "mock shared vars"
  
  it "Credential - Create - Openstack V2 Credential" do 
    with_environment 'DEBUG' => '1' do
      result = cb.credential.create.openstack.keystone_v2.name("cli-os-cred").tenant_user(@os_tenant_user).
        tenant_password(@os_tenant_pwd).tenant_name(@os_tenant_name).endpoint(@os_tenant_endpoint).build(false)
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("credential created")    
    end
  end

  it "Credential - Create - AWS Role based" do 
    with_environment 'DEBUG' => '1' do
      result = cb.credential.create.aws.role_based.name("cli-aws-role").role_arn(@aws_role_arn).build(false)
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("credential created")    
    end
  end

    it "Credential - Create - AWS Key based" do 
    with_environment 'DEBUG' => '1' do
      result = cb.credential.create.aws.key_based.name("cli-aws-key").access_key(@aws_access_key).secret_key(@aws_secret_key).build(false)  
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("credential created")    
    end
  end

    it "Credential - Create - Azure" do 
    with_environment 'DEBUG' => '1' do
      result = cb.credential.create.azure.app_based.name("cli-azure").subscription_id(@az_subscription_id).
      tenant_id(@az_tenant_id).app_id(@az_app_id).app_password(@az_app_pwd).build(false)  
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("credential created")    
    end
  end

    it "Credential - Create - GCP" do 
    with_environment 'DEBUG' => '1' do
   	  create_p12("temp.p12")   	
      result = cb.credential.create.gcp.name("cli-gcp").project_id(@gcp_project_id).service_account_id(@gcp_service_account_id).
      service_account_private_key_file("temp.p12").build(false)
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("credential created")    
    end
  end

    it "Credential - Create - From file" do 
    with_environment 'DEBUG' => '1' do
   	  create_valid_json("mock.json", @valid_cred_json)   	
      result = cb.credential.create.from_file.cli_input_json("mock.json").build(false)
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("credential created")    
    end
  end

    it "Credential - Create - From file - No Name in Json" do 
    with_environment 'DEBUG' => '1' do
   	  create_valid_json("mock.json", @invalid_cred_json)   	
      result = cb.credential.create.from_file.cli_input_json("mock.json").build(false)
      expect(result.exit_status).to eql 1
      expect(result.stderr).to include("Name of the credential must be set ")     
    end
  end

  it "Credential - Describe" do
    result = cb.credential.describe.name("mockcred").build(false)
    expect(result.exit_status).to eql 0
    expect(result.stdout.empty?).to be_falsy
    expect(JSON.parse(result.stdout)).to include_json(
      Name: /.*/,
      Description: /.*/,  
      CloudPlatform: /.*/      
    )       
  end

  it "Credential - List" do
    result = cb.credential.list.build(false)  
    expect(result.exit_status).to eql 0
    expect(result.stdout.empty?).to be_falsy
    JSON.parse(result.stdout).each do |s|    
      expect(s).to include_json(
        Name: /.*/,
        Description: /.*/,  
        CloudPlatform: /.*/   
    )
    end       
  end              
end  