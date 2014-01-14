import os
from policy.assertions import *
from policy.models import API, Policy

try:
  from unittest import TestCase
except ImportError:
  from unittest.case import TestCase

class TestModels(TestCase):

  def test_api_immutability(self):
    api = API('Foo', '1.0', [])
    self.assertEquals('Foo', api.name)
    self.assertEquals('1.0', api.version)
    try:
      api.name = 'Bar'
      self.fail('name attribute not immutable')
    except AttributeError:
      pass

    try:
      api.version = '2.0'
      self.fail('version attribute not immutable')
    except AttributeError:
      pass

  def test_policy_parsing_with_description(self):
    current_dir = os.path.dirname(os.path.abspath(__file__))
    full_path = os.path.join(current_dir, 'samples', 'policy1.py')
    p = Policy(full_path)
    self.assertEquals('policy1', p.name)
    self.assertEquals('This is a comment.', p.description)
    self.assertEquals(full_path, p.policy_file)

  def test_policy_parsing_without_description(self):
    current_dir = os.path.dirname(os.path.abspath(__file__))
    full_path = os.path.join(current_dir, 'samples', 'policy2.py')
    p = Policy(full_path)
    self.assertEquals('policy2', p.name)
    self.assertEquals(None, p.description)
    self.assertEquals(full_path, p.policy_file)

  def test_assert_dependency_1(self):
    api = API('foo', '1.0', [])
    try:
      assert_dependency(api, 'bar', '1.0')
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_dependency_2(self):
    api = API('foo', '1.0', [{'name' : 'bar', 'version' : '1.0'}])
    try:
      assert_dependency(api, 'bar', '1.0')
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

  def test_assert_dependency_3(self):
    api = API('foo', '1.0', [])
    try:
      assert_dependency(api, 'bar')
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_dependency_4(self):
    api = API('foo', '1.0', [{'name' : 'bar', 'version' : '1.0'}])
    try:
      assert_dependency(api, 'bar')
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

  def test_version_comparison(self):
    self.assertTrue(compare_version('1.0', '1.0') is 0)
    self.assertTrue(compare_version('2.0', '1.0') > 0)
    self.assertTrue(compare_version('1.0', '2.0') < 0)

    self.assertTrue(compare_version('1.0.0', '1.0.0') is 0)
    self.assertTrue(compare_version('2.0.0', '1.0.0') > 0)
    self.assertTrue(compare_version('1.0.0', '2.0.0') < 0)

    self.assertTrue(compare_version('2.2', '2.1') > 0)
    self.assertTrue(compare_version('2.2', '2.3') < 0)

    self.assertTrue(compare_version('1.0-rc1', '1.0-rc2') < 0)
    self.assertTrue(compare_version('1.0', '1.0-rc1') > 0)

    self.assertTrue(compare_version('2.0.0-alpha', '2.0.0-beta') < 0)
    self.assertTrue(compare_version('2.0.0-beta', '2.0.0-alpha') > 0)

  def test_assert_dependency_in_range_1(self):
    api = API('foo', '1.0', [])
    try:
      assert_dependency_in_range(api, 'bar', lower='1.0', upper='2.0')
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_dependency_in_range_2(self):
    api = API('foo', '1.0', [{'name' : 'bar', 'version' : '1.5'}])
    try:
      assert_dependency_in_range(api, 'bar', lower='1.0', upper='2.0')
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

  def test_assert_dependency_in_range_3(self):
    api = API('foo', '1.0', [{'name' : 'bar', 'version' : '1.5'}])
    try:
      assert_dependency_in_range(api, 'bar', lower='2.0', upper='3.0')
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_dependency_in_range_4(self):
    api = API('foo', '1.0', [{'name' : 'bar', 'version' : '1.5'}])
    try:
      assert_dependency_in_range(api, 'bar', lower='1.5', upper='3.0', exclude_lower=True)
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_dependency_in_range_5(self):
    api = API('foo', '1.0', [{'name' : 'bar', 'version' : '1.5'}])
    try:
      assert_dependency_in_range(api, 'bar', lower='1.0', upper='1.5', exclude_upper=True)
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_dependency_in_range_6(self):
    api = API('foo', '1.0', [{'name' : 'bar', 'version' : '1.0'}])
    try:
      assert_dependency_in_range(api, 'bar', lower='1.5')
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_dependency_in_range_7(self):
    api = API('foo', '1.0', [{'name' : 'bar', 'version' : '1.5'}])
    try:
      assert_dependency_in_range(api, 'bar', upper='1.0')
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_dependency_in_range_8(self):
    api = API('foo', '1.0', [{'name' : 'bar', 'version' : '1.5'}])
    try:
      assert_dependency_in_range(api, 'bar')
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

    try:
      assert_dependency_in_range(api, 'baz')
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_dependency_in_range_9(self):
    api = API('foo', '1.0', [{'name' : 'bar', 'version' : '1.0'}])
    try:
      assert_dependency_in_range(api, 'bar', lower='1.0')
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

  def test_assert_dependency_in_range_10(self):
    api = API('foo', '1.0', [{'name' : 'bar', 'version' : '1.0'}])
    try:
      assert_dependency_in_range(api, 'bar', upper='1.0')
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

  def test_assert_dependency_in_range_11(self):
    api = API('foo', '1.0', [{'name' : 'bar', 'version' : '1.5'}])
    try:
      assert_dependency_in_range(api, 'bar', lower='1.0', upper='2.0',
        exclude_upper=True, exclude_lower=True)
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

  def test_assert_not_dependency_1(self):
    api = API('foo', '1.0', [])
    try:
      assert_not_dependency(api, 'bar', '1.0')
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

  def test_assert_not_dependency_2(self):
    api = API('foo', '1.0', [{'name' : 'bar', 'version' : '1.0'}])
    try:
      assert_not_dependency(api, 'bar', '1.0')
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_not_dependency_3(self):
    api = API('foo', '1.0', [])
    try:
      assert_not_dependency(api, 'bar')
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

  def test_assert_not_dependency_4(self):
    api = API('foo', '1.0', [{'name' : 'bar', 'version' : '1.0'}])
    try:
      assert_not_dependency(api, 'bar')
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_true(self):
    try:
      assert_true(True)
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

    try:
      assert_true(False)
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_true(self):
    try:
      assert_false(False)
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

    try:
      assert_false(True)
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass
