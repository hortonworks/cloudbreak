require_relative "../common/e2e_vars"
require_relative "../common/command_helpers"
require_relative "spec_helper"
require_relative "../common/cluster_helpers"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "dp "
  return cb
end

RSpec.describe 'Custer operation test cases', :type => :aruba do
  include_context "shared cluster helpers"
  include_context "shared command helpers"
  include_context "e2e shared vars"

  before(:all) do
    result = list_with_name_exists(@os_credential_name)  do
      cb.credential.list.build
    end
    if !(result[0])
      @credential_created = (cb.credential.create.openstack.keystone_v2.name(@os_credential_name).tenant_user(ENV['OS_V2_USERNAME']).
      tenant_password(ENV['OS_V2_PASSWORD']).tenant_name(ENV['OS_V2_TENANT_NAME']).endpoint(ENV['OS_V2_ENDPOINT']).builds).stderr.empty?
    else
      @credential_created = true
    end
  end

  before(:all) do
    result = list_with_name_exists(@environment_name)  do
      cb.env.list.build
    end
    if !(result[0])
      @environment_created = (cb.env.create.name(@environment_name).credential(@os_credential_name).location_name(@environment_location).regions(@environment_regions).build).stderr.empty?
    else
      @environment_created = true
    end
  end

  before(:all) do
    result = list_with_name_exists(@os_cluster_name)  do
      cb.cluster.list.build
    end
    if (result[0])
      result = cb.cluster.delete.name(@os_cluster_name).build
      expect(result).to be_successfully_executed
      expect(wait_for_cluster_deleted(cb, @os_cluster_name)).to be_falsy      
    end
  end  
  
  before(:each) do
    skip("Environment creation failed") unless @environment_created
  end

  after(:all) do 
    result = cb.cluster.delete.name(@os_cluster_name).build
    expect(result).to be_successfully_executed
    expect(wait_for_cluster_deleted(cb, @os_cluster_name)).to be_falsy
    result = cb.env.delete.name(@environment_name).build
    expect(result).to be_successfully_executed
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
      
      for i in 0..5
        result = wait_for_status_cluster(cb, @os_cluster_name, "AVAILABLE")
        expect(result).to eq("AVAILABLE")               
      end   
    end
  end

  xit "Downscale cluster - CB-1050" do
    scale(cb, @os_cluster_name, "compute", -1) do 
      result = wait_for_status_cluster(cb, @os_cluster_name, "UPDATE_IN_PROGRESS")
      expect(result).to eq("UPDATE_IN_PROGRESS")

      result = wait_for_status(cb, @os_cluster_name, "UPDATE_IN_PROGRESS")
      expect(result).to eq("UPDATE_IN_PROGRESS")

      for i in 0..2
        result = wait_for_status(cb, @os_cluster_name, "AVAILABLE")
        expect(result).to eq("AVAILABLE")               
      end   
    end
  end

  it "List clusters - checking created cluster" do
    result = list_with_name_exists(@os_cluster_name)  do
      cb.cluster.list.build
    end
    expect(result[0]).to be_truthy
  end

  it "Change ambari password" do    
    skip_if(cb, @os_cluster_name, "AVAILABLE", "Test is skipped because of cluster is not AVAILABLE")
    result = cb.cluster.change_ambari_password.name(@os_cluster_name).ambari_user(@ambari_user).old_password(@ambari_password).new_password("admintemp").build
    expect(result.exit_status).to eql 0          
  end     
end