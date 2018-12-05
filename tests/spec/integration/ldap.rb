require_relative "../common/mock_vars"
require_relative "../common/command_helpers"
require_relative "spec_helper"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "dp "
  return cb
end

RSpec.describe 'Ldap test cases', :type => :aruba do
  include_context "shared command helpers"    
  include_context "mock shared vars"

  before(:all) do
    result = list_with_name_exists(@ldap_name) do
      cb.ldap.list.build
    end
    if (result[0])
      result = cb.ldap.delete.name(@ldap_name).build
      expect(result.exit_status).to eql 0 
    end
  end
  
  it "Ldap - Create - LDAP directory type" do
    with_environment 'DEBUG' => '1' do 
      result = cb.ldap.create.name(@ldap_name).ldap_server(@ldap_url)
      .ldap_domain(@ldap_domain)
      .ldap_bind_dn(@ldap_bind_dn)
      .ldap_bind_password(@mock_password)
      .ldap_directory_type("LDAP")
      .ldap_user_search_base(@ldap_user_search_base) 
      .ldap_user_dn_pattern(@ldap_user_dn_pattern)
      .ldap_user_name_attribute(@ldap_user_name_attribute) 
      .ldap_user_object_class(@ldap_user_object_class)
      .ldap_group_member_attribute(@ldap_group_member_attribute) 
      .ldap_group_name_attribute(@ldap_group_name_attribute) 
      .ldap_group_object_class(@ldap_group_object_class)
      .ldap_group_search_base(@ldap_group_search_base).build()
    
     expect(result.stderr.to_s.downcase).not_to include("failed", "error")
    end
  end

  it "Ldap - Create - AD directory type" do
    with_environment 'DEBUG' => '1' do 
      result = cb.ldap.create.name(@ldap_name).ldap_server(@ldap_url)
      .ldap_domain(@ldap_domain)
      .ldap_bind_dn(@ldap_bind_dn)
      .ldap_bind_password(@mock_password)
      .ldap_directory_type("ACTIVE_DIRECTORY")
      .ldap_user_search_base(@ldap_user_search_base) 
      .ldap_user_dn_pattern(@ldap_user_dn_pattern)
      .ldap_user_name_attribute(@ldap_user_name_attribute) 
      .ldap_user_object_class(@ldap_user_object_class)
      .ldap_group_member_attribute(@ldap_group_member_attribute) 
      .ldap_group_name_attribute(@ldap_group_name_attribute) 
      .ldap_group_object_class(@ldap_group_object_class)
      .ldap_group_search_base(@ldap_group_search_base).build()
    
     expect(result.stderr.to_s.downcase).not_to include("failed", "error")
    end
  end

  it "Ldap - Create - Invalid ldap url - No port" do
    with_environment 'DEBUG' => '1' do 
      result = cb.ldap.create.name(@ldap_name).ldap_server("ldap://mock-12345.elb.eu-west-1.amazonaws.com")
      .ldap_domain(@ldap_domain)
      .ldap_bind_dn(@ldap_bind_dn)
      .ldap_bind_password(@mock_password)
      .ldap_directory_type("ACTIVE_DIRECTORY")
      .ldap_user_search_base(@ldap_user_search_base) 
      .ldap_user_dn_pattern(@ldap_user_dn_pattern)
      .ldap_user_name_attribute(@ldap_user_name_attribute) 
      .ldap_user_object_class(@ldap_user_object_class)
      .ldap_group_member_attribute(@ldap_group_member_attribute) 
      .ldap_group_name_attribute(@ldap_group_name_attribute) 
      .ldap_group_object_class(@ldap_group_object_class)
      .ldap_group_search_base(@ldap_group_search_base).build(false)
     
     expect(result.exit_status).to eql 1 
     expect(result.stderr.to_s.downcase).to include("error")
    end
  end

  it "Ldap - Create - Invalid ldap url - No protocol" do
    with_environment 'DEBUG' => '1' do 
      result = cb.ldap.create.name(@ldap_name).ldap_server("mock-12345.elb.eu-west-1.amazonaws.com:89")
      .ldap_domain(@ldap_domain)
      .ldap_bind_dn(@ldap_bind_dn)
      .ldap_bind_password(@mock_password)
      .ldap_directory_type("ACTIVE_DIRECTORY")
      .ldap_user_search_base(@ldap_user_search_base) 
      .ldap_user_dn_pattern(@ldap_user_dn_pattern)
      .ldap_user_name_attribute(@ldap_user_name_attribute) 
      .ldap_user_object_class(@ldap_user_object_class)
      .ldap_group_member_attribute(@ldap_group_member_attribute) 
      .ldap_group_name_attribute(@ldap_group_name_attribute) 
      .ldap_group_object_class(@ldap_group_object_class)
      .ldap_group_search_base(@ldap_group_search_base).build(false)
     
     expect(result.exit_status).to eql 1 
     expect(result.stderr.to_s.downcase).to include("error")
    end
  end

  it "Ldap - List" do
    result = cb.ldap.list.build(false)
    expect(result.stdout.empty?).to be_falsy, "Json should't be empty"

    JSON.parse(result.stdout).each do |s|    
        expect(s).to include_json(
        Name: /.*/,
        Description: /.*/,
        Server: /.*/,
        Domain: /.*/,
        DirectoryType: /.*/,
        UserSearchBase: /.*/,
        UserDnPattern: /.*/,
        UserNameAttribute: /.*/,
        UserObjectClass: /.*/,  
        GroupMemberAttribute: /.*/,
        GroupNameAttribute: /.*/,
        GroupObjectClass: /.*/,
        GroupSearchBase: /.*/,
        AdminGroup: /.*/,
        Environments: /.*/
      )
    end
  end

  it "Database - Delete" do
    with_environment 'DEBUG' => '1' do    
      result = cb.ldap.delete.name(@ldap_name).build
      expect(result.exit_status).to eql 0 
    end   
  end          
end  