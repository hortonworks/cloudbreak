require 'aruba/rspec'
require "rspec/json_expectations"
require 'json'
require "common/command_builder.rb"
require "oauth"

def html_print(&blk)
  puts "<pre>"
  blk.call
  puts "</pre>"
end