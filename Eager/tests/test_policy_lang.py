from policy.policy_language import validate_policy

try:
  from unittest import TestCase
except ImportError:
  from unittest.case import TestCase

class TestPolicyLanguage(TestCase):
  def test_parser_1(self):
    source = """assert_dependency(api, 'Foo', '1.0')"""
    try:
      validate_policy(source)
    except Exception as ex:
      self.fail("Unexpected error")

  def test_parser_2(self):
    source = """assert_dependency(api, 'Foo', '1.0')
open('foo.txt','r')"""
    try:
      validate_policy(source)
      self.fail("Invalid function did not throw exception")
    except Exception as ex:
      pass

  def test_parser_3(self):
    source = """class Foo:
  pass"""
    try:
      validate_policy(source)
      self.fail("Class definition did not throw exception")
    except Exception as ex:
      pass

  def test_parser_4(self):
    source = """def foo():
 pass

foo()"""
    try:
      validate_policy(source)
    except Exception as ex:
      print ex
      self.fail("Unexpected error")

  def test_parser_5(self):
    source = """def foo(val):
pass

foo(bar())"""
    try:
      validate_policy(source)
      self.fail("Invalid function did not throw exception")
    except Exception as ex:
      pass

  def test_parser_6(self):
    source = """if api.owner == 'alice':
  assert_not_dependency(api, 'Foo', '1.0')"""
    try:
      validate_policy(source)
    except Exception as ex:
      self.fail("Unexpected error")

  def test_parser_7(self):
    source = """from re import compile
compile('^foo')"""
    try:
      validate_policy(source)
    except Exception as ex:
      print ex
      self.fail("Unexpected error")

  def test_parser_8(self):
    source = """import re
re.compile('^foo')"""
    try:
      validate_policy(source)
    except Exception as ex:
      print ex
      self.fail("Unexpected error")

  def test_parser_9(self):
    source = """import os"""
    try:
      validate_policy(source)
      self.fail("Invalid module did not throw exception")
    except Exception as ex:
      pass

  def test_parser_10(self):
    source = """from os import *"""
    try:
      validate_policy(source)
      self.fail("Invalid module did not throw exception")
    except Exception as ex:
      pass

  def test_parser_11(self):
    source = """import __builtin__"""
    try:
      validate_policy(source)
      self.fail("Invalid module did not throw exception")
    except Exception as ex:
      pass