from policy.policy_language import validate_policy

try:
  from unittest import TestCase
except ImportError:
  from unittest.case import TestCase

class TestPolicyLanguage(TestCase):

  def test_parser_1(self):
    source = """assert_dependency(api, 'Foo', '1.0')"""
    status, message = validate_policy(source)
    self.assertTrue(status)

  def test_parser_2(self):
    source = """assert_dependency(api, 'Foo', '1.0')
open('foo.txt','r')"""
    status, message = validate_policy(source)
    self.assertFalse(status)

  def test_parser_3(self):
    source = """class Foo:
  pass"""
    status, message = validate_policy(source)
    self.assertFalse(status)

  def test_parser_4(self):
    source = """def foo():
 pass

foo()"""
    status, message = validate_policy(source)
    self.assertTrue(status)

  def test_parser_5(self):
    source = """def foo(val):
 pass

foo(bar())"""
    status, message = validate_policy(source)
    self.assertFalse(status)

  def test_parser_6(self):
    source = """if api.owner == 'alice':
  assert_not_dependency(api, 'Foo', '1.0')"""
    status, message = validate_policy(source)
    self.assertTrue(status)