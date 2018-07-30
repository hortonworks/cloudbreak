require_relative "../common/e2e_vars"
require_relative "../common/command_helpers"
require_relative "spec_helper"

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

  before(:all) do
    result = list_with_name_exists(@os_credential_name + "-mod") do
      cb.credential.list.build
    end
    if (result[0])
      result = cb.credential.delete.name(@os_credential_name + "-mod").build
      expect(result.exit_status).to eql 0
    end
  end

  before(:all) do
    result = list_with_name_exists(@aws_credential_name) do
      cb.credential.list.build
    end
    if (result[0])
      result = cb.credential.delete.name(@aws_credential_name).build
      expect(result.exit_status).to eql 0
    end
  end

  after(:all) do
    result = cb.credential.delete.name(@os_credential_name + "-mod").build
    expect(result).to be_successfully_executed

    result = cb.credential.delete.name(@aws_credential_name).build
    expect(result).to be_successfully_executed
  end

  it "Credential - Create - Describe - List - Delete - Openstack V2 Credential" do
    credential_create_describe_list_delete(cb, @os_credential_name + "-cred") do
      cb.credential.create.openstack.keystone_v2.name(@os_credential_name + "-cred").tenant_user(ENV['OS_V2_USERNAME']).
        tenant_password(ENV['OS_V2_PASSWORD']).tenant_name(ENV['OS_V2_TENANT_NAME']).endpoint(ENV['OS_V2_ENDPOINT']).builds
    end     
  end

  it "Credential - Create - Modify - Openstack V2 Credential " do
    result = cb.credential.create.openstack.keystone_v2.name(@os_credential_name + "-mod").tenant_user(ENV['OS_V2_USERNAME']).
      tenant_password(ENV['OS_V2_PASSWORD']).tenant_name(ENV['OS_V2_TENANT_NAME']).endpoint(ENV['OS_V2_ENDPOINT']).description("e2etest").builds
    expect(result.exit_status).to eql 0

    result = cb.credential.modify.openstack.keystone_v2.name(@os_credential_name + "-mod").tenant_user(ENV['OS_V2_USERNAME']).
      tenant_password(ENV['OS_V2_PASSWORD']).tenant_name(ENV['OS_V2_TENANT_NAME']).endpoint(ENV['OS_V2_ENDPOINT']).description("modified").builds
    expect(result.exit_status).to eql 0

    result = cb.credential.describe.name(@os_credential_name + "-mod").build
    expect(result.exit_status).to eql 0
    expect(result.stdout.empty?).to be_falsy
    expect(result.stdout).to include_json(
      Description: "modified"
    )
  end

  it "Credential - Modify - Not valid credential - Not existing" do
    result = cb.credential.modify.openstack.keystone_v2.name("notexisting").tenant_user(ENV['OS_V2_USERNAME']).
      tenant_password(ENV['OS_V2_PASSWORD']).tenant_name(ENV['OS_V2_TENANT_NAME']).endpoint(ENV['OS_V2_ENDPOINT']).description("modified").builds
    expect(result.exit_status).to eql 1
    expect(result.stderr.to_s.downcase).to include("error")
  end

  it "Credential - Create - Aws - Modify with openstack params" do
    result = cb.credential.create.aws.role_based.name(@aws_credential_name).role_arn(ENV['AWS_ROLE_ARN']).builds
    expect(result.exit_status).to eql 0

    result = cb.credential.modify.openstack.keystone_v2.name(@aws_credential_name).tenant_user(ENV['OS_V2_USERNAME']).
      tenant_password(ENV['OS_V2_PASSWORD']).tenant_name(ENV['OS_V2_TENANT_NAME']).endpoint(ENV['OS_V2_ENDPOINT']).description("modified").builds
    expect(result.exit_status).to eql 1
    expect(result.stderr.to_s.downcase).to include("error")
  end
end