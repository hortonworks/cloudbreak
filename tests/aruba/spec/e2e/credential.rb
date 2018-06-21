require "common/e2e_vars.rb"
require "common/command_helpers.rb"
require "e2e/spec_helper"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "cb "
  return cb
end

RSpec.describe 'Credential test cases', :type => :aruba do
  include_context "shared command helpers"    
  include_context "e2e shared vars"

  before(:all) do
    result = list_with_name_exists(@os_credential_name + "-cred") do
      cb.credential.list.build
    end
    if (result[0])
      result = cb.credential.delete.name(@os_credential_name + "-cred").build
      expect(result.exit_status).to eql 0 
    end
  end

  it "Credential - Create - Describe - List - Delete - Openstack V2 Credential" do 
    credential_create_describe_list_delete(cb, @os_credential_name + "-cred") do
      cb.credential.create.openstack.keystone_v2.name(@os_credential_name + "-cred").tenant_user(ENV['OS_V2_USERNAME']).
        tenant_password(ENV['OS_V2_PASSWORD']).tenant_name(ENV['OS_V2_TENANT_NAME']).endpoint(ENV['OS_V2_ENDPOINT']).builds
    end     
  end
end  