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