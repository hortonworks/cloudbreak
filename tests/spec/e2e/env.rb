require_relative "../common/e2e_vars"
require_relative "../common/command_helpers"
require_relative "spec_helper"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "dp "
  return cb
end

RSpec.describe 'Environment test cases', :type => :aruba do
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
    result = list_with_name_exists(@environment_name) do
      cb.env.list.build
    end
    if (result[0])
      result = cb.env.delete.name(@environment_name).build
      expect(result.exit_status).to eql 0 
    end
  end

  it "Environment - Create - Without additional params" do
    result = cb.env.create.name(@environment_name).credential(@os_credential_name).location_name(@environment_location).regions(@environment_regions).build
    expect(result.exit_status).to eql 0 
  end 

  it "Environment - List - Checking previously created environment" do
    result = list_with_name_exists(@environment_name) do
      cb.env.list.build
    end
    expect(result[0]).to be_truthy

    result[1].each do |s|
      if s["Name"] ==  @environment_name
        expect(s).to include_json(
          Name: @environment_name,
          Credential: @os_credential_name,
          CloudPlatform: "OPENSTACK",
          LocationName: @environment_location,
        )
      end
    end        
  end
end