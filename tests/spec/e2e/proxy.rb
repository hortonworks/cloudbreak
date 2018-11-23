require_relative "../common/e2e_vars"
require_relative "../common/command_helpers"
require_relative "spec_helper"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "dp "
  return cb
end

RSpec.describe 'Proxy test cases', :type => :aruba do
  include_context "shared command helpers"    
  include_context "e2e shared vars"

  before(:all) do
    result = list_with_name_exists(@proxy_name) do
      cb.proxy.list.build
    end
    if (result[0])
      result = cb.proxy.delete.name(@proxy_name).build
      expect(result.exit_status).to eql 0 
    end
  end

  before(:all) do
    result = list_with_name_exists(@proxy_name + "-extra") do
      cb.proxy.list.build
    end
    if (result[0])
      result = cb.proxy.delete.name(@proxy_name + "-extra").build
      expect(result.exit_status).to eql 0 
    end
  end

   before(:all) do
    result = list_with_name_exists(@proxy_name + "-https") do
      cb.proxy.list.build
    end
    if (result[0])
      result = cb.proxy.delete.name(@proxy_name + "-https").build
      expect(result.exit_status).to eql 0 
    end
  end

  it "Proxy - Create - Without additional params" do 
    result = cb.proxy.create.name(@proxy_name).proxy_host(@proxy_server).proxy_port(@proxy_port).build  
    expect(result.exit_status).to eql 0 
  end 

  it "Proxy - List - Checking previosly created proxy" do
    result = list_with_name_exists(@proxy_name) do
      cb.proxy.list.build
    end
    expect(result[0]).to be_truthy

    result[1].each do |s|
      if s["Name"] ==  @proxy_name          
        expect(s).to include_json(
          Name: @proxy_name,
          Host: @proxy_server,  
          Port: @proxy_port,
          Protocol: "http",
          User: /.*/
        )
      end
    end        
  end

  it "Proxy - Create - With additional params" do 
    result = cb.proxy.create.name(@proxy_name + "-extra").proxy_host(@proxy_server).proxy_port(@proxy_port).proxy_user(@proxy_user)
    .proxy_password(@proxy_password).build
    expect(result.exit_status).to eql 0 
  end

  it "Proxy - List - Checking previosly created proxy and cheking User name" do
    result = list_with_name_exists(@proxy_name) do
      cb.proxy.list.build
    end
    expect(result[0]).to be_truthy

    result[1].each do |s| 
      if s["Name"] ==  @proxy_name + "-extra" 
        expect(s).to include_json(
          Name: /.*/,
          Host: /.*/,  
          Port: /.*/,
          Protocol: /.*/,
          User: @proxy_user
        )
     end
    end        
  end

  it "Proxy - Create - With additional params - Https protocol" do 
    result = cb.proxy.create.name(@proxy_name + "-https").proxy_host(@proxy_server).proxy_port(@proxy_port).proxy_user(@proxy_user)
    .proxy_password(@proxy_password).proxy_protocol("https").build
    expect(result.exit_status).to eql 0 
  end

  it "Proxy - List - Checking previosly created proxy and cheking protocol" do
    result = list_with_name_exists(@proxy_name) do
      cb.proxy.list.build
    end
    expect(result[0]).to be_truthy

    result[1].each do |s| 
      if s["Name"] ==  @proxy_name + "-https" 
        expect(s).to include_json(
          Name: /.*/,
          Host: /.*/,  
          Port: /.*/,
          Protocol: "https",
          User: /.*/
        )
     end
    end        
  end          

  it "Proxy - Delete - Previously created proxies" do
    result = cb.proxy.delete.name(@proxy_name).build
    expect(result.exit_status).to eql 0

    result = cb.proxy.delete.name(@proxy_name  + "-extra").build
    expect(result.exit_status).to eql 0 

    result = cb.proxy.delete.name(@proxy_name  + "-https").build
    expect(result.exit_status).to eql 0                   
  end      
end  