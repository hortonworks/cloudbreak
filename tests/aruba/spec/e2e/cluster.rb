require "common/e2e_vars.rb"
require "common/helpers.rb"
require "common/command_helpers.rb"
require "e2e/spec_helper"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "cb "
  return cb
end

RSpec.describe 'Custer operation test cases', :type => :aruba do
  include_context "shared helpers"
  include_context "shared command helpers"
  include_context "shared vars"
  
  before(:all) do
    @credential_created = (cb.credential.create.openstack.keystone_v2.name(@os_credential_name).tenant_user(ENV['OS_V2_USERNAME']).
       tenant_password(ENV['OS_V2_PASSWORD']).tenant_name(ENV['OS_V2_TENANT_NAME']).endpoint(ENV['OS_V2_ENDPOINT']).build).stderr.empty?
  end
  
  before(:each) do
    skip("Credential creation failed") unless @credential_created 
  end

  after(:all) do 
    result = cb.cluster.delete.name(@os_cluster_name).build
    expect(result).to be_successfully_executed
    expect(wait_for_cluster_deleted(cb, @os_cluster_name)).to be_falsy
    result = cb.credential.delete.name(@os_credential_name).build
    expect(result).to be_successfully_executed    
  end

  it "Create cluster" do
    result = cb.cluster.create.name(@os_cluster_name).cli_input_json(@cli_input_json).build
    get_cluster_info(cb, @os_cluster_name)
    expect(result.exit_status).to eql 0
    expect(wait_for_status(cb, @os_cluster_name, "AVAILABLE")).to eq("AVAILABLE") 
  end

  it "Stop cluster" do
    skip_if(cb, @os_cluster_name, "AVAILABLE", "Test is skipped because of cluster is not AVAILABLE")
    result = cb.cluster.stop.name(@os_cluster_name).build    
    expect(result.exit_status).to eql 0
    expect(wait_for_status(cb, @os_cluster_name, "STOPPED")).to eq("STOPPED")       
  end

  it "Start cluster" do
    skip_if(cb, @os_cluster_name, "STOPPED", "Test is skipped because of cluster is not STOPPED")    
    result = cb.cluster.start.name(@os_cluster_name).build          
    expect(result.exit_status).to eql 0
    expect(wait_for_status(cb, @os_cluster_name, "AVAILABLE")).to eq("AVAILABLE")      
  end

  it "Upscale cluster" do
    scale(cb, @os_cluster_name, "compute", 1) do 
      result = wait_for_status(cb, @os_cluster_name, "UPDATE_IN_PROGRESS")
      expect(result).to eq("UPDATE_IN_PROGRESS")

      result = wait_for_status_cluster(cb, @os_cluster_name, "UPDATE_IN_PROGRESS")
      expect(result).to eq("UPDATE_IN_PROGRESS")

      result = wait_for_status_cluster(cb, @os_cluster_name, "AVAILABLE")
      expect(result).to eq("AVAILABLE")               

      result = wait_for_status_cluster(cb, @os_cluster_name, "AVAILABLE")
      expect(result).to eq("AVAILABLE")    
    end
  end

  it "Downscale cluster" do
    scale(cb, @os_cluster_name, "compute", -1) do 
      result = wait_for_status_cluster(cb, @os_cluster_name, "UPDATE_IN_PROGRESS")
      expect(result).to eq("UPDATE_IN_PROGRESS")

      result = wait_for_status(cb, @os_cluster_name, "UPDATE_IN_PROGRESS")
      expect(result).to eq("UPDATE_IN_PROGRESS")

      result = wait_for_status(cb, @os_cluster_name, "AVAILABLE")
      expect(result).to eq("AVAILABLE")

    end
  end

  it "List clusters - checking created cluster" do
    expect(cluster_exists(cb, @os_cluster_name)).to be_truthy
  end

  it "Change ambari password" do    
    result = cb.cluster.change_ambari_password.name(@os_cluster_name).ambari_user(@ambari_user).old_password(@ambari_password).new_password("admintemp").build
    expect(result.exit_status).to eql 0          
  end

  it "Generate reinstall template" do 
    result = cb.cluster.generate_reinstall_template.name(@os_cluster_name).blueprint_name(@default_blueprint_name).build
    expect(result.exit_status).to eql 0  
    expect(JSON.parse(result.stdout)["blueprintName"]).to eq(@default_blueprint_name.gsub("'",""))    
  end           
end
