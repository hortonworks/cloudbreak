require_relative "../common/e2e_vars"
require_relative "../common/command_helpers"
require_relative "spec_helper"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "dp "
  return cb
end

RSpec.describe 'Ldap test cases', :type => :aruba do
  include_context "shared command helpers"    
  include_context "e2e shared vars"

  before(:all) do
    result = list_with_name_exists(@ldap_name) do
      cb.ldap.list.build
    end
    if (result[0])
      result = cb.ldap.delete.name(@ldap_name).build
      expect(result.exit_status).to eql 0 
    end
  end
  
  it "Ldap - Create " do 
    result = cb.ldap.create.name(@ldap_name).ldap_server("ldap://" + ENV['INTEGRATIONTEST_LDAPCONFIG_LDAPSERVERHOST'] + ":89")
    .ldap_domain(@ldap_domain)
    .ldap_bind_dn(@ldap_bind_dn)
    .ldap_bind_password(ENV['INTEGRATIONTEST_LDAPCONFIG_BINDPASSWORD'])
    .ldap_directory_type("LDAP")
    .ldap_user_search_base(@ldap_user_search_base) 
    .ldap_user_dn_pattern(@ldap_user_dn_pattern)
    .ldap_user_name_attribute(@ldap_user_name_attribute) 
    .ldap_user_object_class(@ldap_user_object_class)
    .ldap_group_member_attribute(@ldap_group_member_attribute) 
    .ldap_group_name_attribute(@ldap_group_name_attribute) 
    .ldap_group_object_class(@ldap_group_object_class)
    .ldap_group_search_base(@ldap_group_search_base).builds()
    
    expect(result.exit_status).to eql 0 
  end 

  it "Ldap - List - Checking previosly created ldap" do
    result = list_with_name_exists(@ldap_name) do
      cb.ldap.list.build
    end
    expect(result[0]).to be_truthy

    result[1].each do |s|    
      expect(s).to include_json(
        Name: /.*/,
        Server: /.*/,  
        BindDn: /.*/,
        UserSearchBase: /.*/,
        UserDnPattern: /.*/,
        UserNameAttribute: /.*/,
        UserObjectClass: /.*/,  
        GroupMemberAttribute: /.*/,
        GroupNameAttribute: /.*/,
        GroupObjectClass: /.*/,
        GroupSearchBase: /.*/                
      )
    end        
  end 

  it "Ldap - Delete - Previously created ldap" do
    result = cb.ldap.delete.name(@ldap_name).build
    expect(result.exit_status).to eql 0    
  end 
end  