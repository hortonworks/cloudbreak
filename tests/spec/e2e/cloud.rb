require_relative "../common/e2e_vars"
require_relative "../common/command_helpers"
require_relative "spec_helper"
require_relative "../common/helpers"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "cb "
  return cb
end

RSpec.describe 'Cloud test cases', :type => :aruba do
  include_context "shared helpers"
  include_context "shared command helpers"    
  include_context "shared vars"

  before(:all) do
    @credential_exist = credential_list_with_check(@os_credential_name + "-cloud") 
    if !(@credential_exist)
      result = cb.credential.create.openstack.keystone_v2.name(@os_credential_name + "-cloud").tenant_user(ENV['OS_V2_USERNAME']).
          tenant_password(ENV['OS_V2_PASSWORD']).tenant_name(ENV['OS_V2_TENANT_NAME']).endpoint(ENV['OS_V2_ENDPOINT']).build
      expect(result.exit_status).to eql 0 
    end
    result = cb.cloud.regions.credential(@os_credential_name + "-cloud").build
    @os_region = get_region(result.stdout)
  end
  
  after(:all) do 
    result = cb.credential.delete.name(@os_credential_name + "-cloud").build
    expect(result).to be_successfully_executed    
  end 

  it "Cloud - Availability zones list" do 
    result = cb.cloud.availability_zones.credential(@os_credential_name + "-cloud").region(@os_region).build
    expect(result.exit_status).to eql 0
    expect(result.stdout.empty?).to be_falsy
    json = JSON.parse(result.stdout)
    json.each do |s| 
      expect(s).to include_json(
         Name: /.*/  
      ) 
    end
  end

  it "Cloud - Instances list" do 
    result = cb.cloud.instances.credential(@os_credential_name + "-cloud").region(@os_region).build
    expect(result.exit_status).to eql 0
  end 

  it "Cloud - Volumes list AWS" do 
    result = cb.cloud.volumes.aws.build
    expect(result.exit_status).to eql 0
    expect(result.stdout.empty?).to be_falsy
    json = JSON.parse(result.stdout)
    json.each do |s| 
      expect(s).to include_json(
        Name: /.*/,
        Description: /.*/  
      ) 
    end
  end  

  it "Cloud - Volumes list Azure" do 
    result = cb.cloud.volumes.azure.build
    expect(result.exit_status).to eql 0
    expect(result.stdout.empty?).to be_falsy
    json = JSON.parse(result.stdout)
    json.each do |s| 
      expect(s).to include_json(
        Name: /.*/,
        Description: /.*/  
      ) 
    end
  end              

  it "Cloud - Volumes list GCP" do 
    result = cb.cloud.volumes.gcp.build
    expect(result.exit_status).to eql 0
    expect(result.stdout.empty?).to be_falsy
    json = JSON.parse(result.stdout)
    json.each do |s| 
      expect(s).to include_json(
        Name: /.*/,
        Description: /.*/  
      ) 
    end
  end   
end  