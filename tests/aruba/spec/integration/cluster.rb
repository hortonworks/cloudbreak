require_relative "../common/mock_vars"
require_relative "../common/command_helpers"
require_relative "spec_helper"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "cb "
  return cb
end

RSpec.describe 'Cluster test cases', :type => :aruba do
  include_context "shared command helpers"    
  include_context "mock shared vars"


  it "Cluster - List" do
    result = cb.cluster.list.build(false) 
    expect(result.exit_status).to eql 0
    expect(result.stdout.empty?).to be_falsy
    JSON.parse(result.stdout).each do |s|    
      expect(s).to include_json(
        Name: /.*/,
        Description: /.*/,  
        CloudPlatform: /.*/,
        StackStatus: /.*/,
        ClusterStatus: /.*/     
    )
    end       
  end     

  it "Cluster - Create" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.create.name(@os_cluster_name).cli_input_json(@cli_input_json).build(false)
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("stack created")    
    end
  end

  it "Cluster - Create - Without Name" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.create.name("").cli_input_json(@cli_input_json).build(false) 
      expect(result.exit_status).to eql 1
      expect(result.stderr).to include("missing")    
    end
  end

  it "Cluster - Create - Without password" do 
    skip("BUG-94445")
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.create.name("asdfg").cli_input_json("../../templates/template-wo-pwd.json").build(false)
      expect(result.exit_status).to eql 1
      expect(result.stderr).to include("ambariRequest password may not be null")          
    end
  end

  it "Cluster - Create - Without password by parameter" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.create.name("asdfg").cli_input_json("../../templates/template-wo-pwd.json").input_json_param_password("pass123").build(false) 
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("stack created")          
    end
  end      

  it "Cluster - Describe Success" do
    result = cb.cluster.describe.name("openstack-cluster").build(false) 
    expect(result.exit_status).to eql 0
    expect(JSON.parse(result.stdout)["name"]).to eql "openstack-cluster"    
  end

  it "Cluster - Describe Failure" do
    result = cb.cluster.describe.name("az404").build(false)   
    expect(result.exit_status).to eql 1
    expect(result.stderr).to include("error")
    expect(result.stderr).to include("az404")    
  end

   it "Cluster - Stop - Success" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.stop.name("asdfg").build(false) 
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("stop stack")          
    end
  end

   it "Cluster - Stop - Failure" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.stop.name("azstatus").build(false)
      expect(result.exit_status).to eql 1
      expect(result.stderr).to include("Stack 'azstatus' not found")          
    end
  end

    it "Cluster - Start - Success" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.start.name("asdfg").build(false)
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("start stack")          
    end
  end

   it "Cluster - Start - Failure" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.start.name("azstatus").build(false)
      expect(result.exit_status).to eql 1
      expect(result.stderr).to include("Stack 'azstatus' not found")          
    end   
  end

      it "Cluster - Sync - Success" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.sync.name("asdfg").build(false)
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("stack synced")          
    end
  end

   it "Cluster - Sync - Failure" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.sync.name("azstatus").build(false)
      expect(result.exit_status).to eql 1
      expect(result.stderr).to include("Stack 'azstatus' not found")          
    end   
  end

    it "Cluster - Repair - Success" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.repair.name("asdfg").host_groups("test").build(false)
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("stack repaired")          
    end
  end

   xit "Cluster - Repair - Failure" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.repair.name("azstatus").host_groups("test").build(false)
      expect(result.exit_status).to eql 1
      expect(result.stderr).to include("Stack 'azstatus' not found")          
    end   
  end      
  
    it "Cluster - Scale - Success" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.scale.name("climock").group_name("hgroup").desired_node_count(3).build(false)             
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("stack scaled")          
    end
  end

   it "Cluster - Scale - Failure" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.scale.name("azstatus").group_name("hgroup").desired_node_count(3).build(false)   
      expect(result.exit_status).to eql 1
      expect(result.stderr).to include("Stack 'azstatus' not found")          
    end                   
  end

    it "Cluster - Reinstall - Success" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.reinstall.name("asdfg").cli_input_json(@cli_input_json).blueprint_name("asdfg").build(false)
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("reinstall stack took")          
    end
  end

    it "Cluster - Generate template - AWS - New network" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.generate_template.aws.new_network.build(false)
      expect(result.exit_status).to eql 0
      expect(result.stdout.empty?).to be_falsy        
    end
  end

    it "Cluster - Generate template - AWS - Existing network" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.generate_template.aws.existing_network.build(false)
      expect(result.exit_status).to eql 0
      expect(result.stdout.empty?).to be_falsy        
    end
  end 

    it "Cluster - Generate template - AWS - New subnet" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.generate_template.aws.existing_subnet.build(false)
      expect(result.exit_status).to eql 0
      expect(result.stdout.empty?).to be_falsy        
    end
  end     

    it "Cluster - Generate template - Azure - New network" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.generate_template.azure.new_network.build(false)  
      expect(result.exit_status).to eql 0
      expect(result.stdout.empty?).to be_falsy        
    end
  end


    it "Cluster - Generate template - Azure - New subnet" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.generate_template.azure.existing_subnet.build(false) 
      expect(result.exit_status).to eql 0
      expect(result.stdout.empty?).to be_falsy        
    end
  end

    it "Cluster - Generate template - Gcp - New network" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.generate_template.gcp.new_network.build(false)
      expect(result.exit_status).to eql 0
      expect(result.stdout.empty?).to be_falsy        
    end
  end

    it "Cluster - Generate template - Gcp - Existing network" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.generate_template.gcp.existing_network.build(false) 
      expect(result.exit_status).to eql 0 
      expect(result.stdout.empty?).to be_falsy       
    end
  end 

    it "Cluster - Generate template - Gcp - New subnet" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.generate_template.gcp.existing_subnet.build(false)
      expect(result.exit_status).to eql 0
      expect(result.stdout.empty?).to be_falsy        
    end
  end 

    it "Cluster - Generate template - Openstack - New network" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.generate_template.openstack.new_network.build(false) 
      expect(result.exit_status).to eql 0
      expect(result.stdout.empty?).to be_falsy        
    end
  end

    it "Cluster - Generate template - Openstack - Existing network" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.generate_template.openstack.existing_network.build(false) 
      expect(result.exit_status).to eql 0 
      expect(result.stdout.empty?).to be_falsy       
    end
  end 

    it "Cluster - Generate template - Openstack - New subnet" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.generate_template.openstack.existing_subnet.build(false)
      expect(result.exit_status).to eql 0
      expect(result.stdout.empty?).to be_falsy       
    end
  end 

    it "Cluster - Generate re-install template " do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.generate_reinstall_template.name("test").blueprint_name("test").build(false)
      expect(result.exit_status).to eql 0 
      expect(result.stdout.empty?).to be_falsy      
    end
  end

    xit "Cluster - Generate attached cluster template " do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.generate_attached_cluster_template.source_cluster("dl-ok").blueprint_name("test").build(false)
      expect(result.exit_status).to eql 0
      expect(result.stdout.empty?).to be_falsy 
    end
  end

    it "Cluster - Generate attached cluster template - Not datalake cluster" do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.generate_attached_cluster_template.source_cluster("test").blueprint_name("test").build(false)
      expect(result.exit_status).to eql 1
      expect(result.stderr).to include("error")        
    end
  end  

    it "Cluster - Re-try " do 
    with_environment 'DEBUG' => '1' do
      result = cb.cluster.retry.name("test").build(false)
      expect(result.exit_status).to eql 0 
    end
  end                 
end  
