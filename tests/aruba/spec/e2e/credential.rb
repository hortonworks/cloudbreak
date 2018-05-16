require "common/e2e_vars.rb"
require "common/helpers.rb"
require "common/command_helpers.rb"
require "e2e/spec_helper"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "cb "
  return cb
end

RSpec.describe 'Credential test cases', :type => :aruba do
  include_context "shared helpers"
  include_context "shared command helpers"    
  include_context "shared vars"

  it "Credential - Create - Describe - List - Delete- Openstack V2 Credential" do 
    credential_create_describe_list_delete(cb, "cli-os-cred") do
      cb.credential.create.openstack.keystone_v2.name("cli-os-cred").tenant_user(ENV['OS_V2_USERNAME']).
        tenant_password(ENV['OS_V2_PASSWORD']).tenant_name(ENV['OS_V2_TENANT_NAME']).endpoint(ENV['OS_V2_ENDPOINT']).build
    end     
  end

end  