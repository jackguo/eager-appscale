from policy.assertions import *
from policy.models import Application

try:
  from unittest import TestCase
except ImportError:
  from unittest.case import TestCase

class TestAssertions(TestCase):

  def test_assert_dependency_1(self):
    app = Application('foo', '1.0', [], [], 'admin')
    try:
      assert_app_dependency(app, 'bar', '1.0')
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_dependency_2(self):
    app = Application('foo', '1.0', [{'name' : 'bar', 'version' : '1.0'}], [], 'admin')
    try:
      assert_app_dependency(app, 'bar', '1.0')
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

  def test_assert_dependency_3(self):
    app = Application('foo', '1.0', [], [], 'admin')
    try:
      assert_app_dependency(app, 'bar')
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_dependency_4(self):
    app = Application('foo', '1.0', [{'name' : 'bar', 'version' : '1.0'}], [], 'admin')
    try:
      assert_app_dependency(app, 'bar')
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

  def test_assert_dependency_in_range_1(self):
    app = Application('foo', '1.0', [], [], 'admin')
    try:
      assert_app_dependency_in_range(app, 'bar', lower='1.0', upper='2.0')
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_dependency_in_range_2(self):
    app = Application('foo', '1.0', [{'name' : 'bar', 'version' : '1.5'}], [], 'admin')
    try:
      assert_app_dependency_in_range(app, 'bar', lower='1.0', upper='2.0')
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

  def test_assert_dependency_in_range_3(self):
    app = Application('foo', '1.0', [{'name' : 'bar', 'version' : '1.5'}], [], 'admin')
    try:
      assert_app_dependency_in_range(app, 'bar', lower='2.0', upper='3.0')
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_dependency_in_range_4(self):
    app = Application('foo', '1.0', [{'name' : 'bar', 'version' : '1.5'}], [], 'admin')
    try:
      assert_app_dependency_in_range(app, 'bar', lower='1.5', upper='3.0', exclude_lower=True)
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_dependency_in_range_5(self):
    app = Application('foo', '1.0', [{'name' : 'bar', 'version' : '1.5'}], [], 'admin')
    try:
      assert_app_dependency_in_range(app, 'bar', lower='1.0', upper='1.5', exclude_upper=True)
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_dependency_in_range_6(self):
    app = Application('foo', '1.0', [{'name' : 'bar', 'version' : '1.0'}], [], 'admin')
    try:
      assert_app_dependency_in_range(app, 'bar', lower='1.5')
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_dependency_in_range_7(self):
    app = Application('foo', '1.0', [{'name' : 'bar', 'version' : '1.5'}], [], 'admin')
    try:
      assert_app_dependency_in_range(app, 'bar', upper='1.0')
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_dependency_in_range_8(self):
    app = Application('foo', '1.0', [{'name' : 'bar', 'version' : '1.5'}], [], 'admin')
    try:
      assert_app_dependency_in_range(app, 'bar')
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

    try:
      assert_app_dependency_in_range(app, 'baz')
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_dependency_in_range_9(self):
    app = Application('foo', '1.0', [{'name' : 'bar', 'version' : '1.0'}], [], 'admin')
    try:
      assert_app_dependency_in_range(app, 'bar', lower='1.0')
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

  def test_assert_dependency_in_range_10(self):
    app = Application('foo', '1.0', [{'name' : 'bar', 'version' : '1.0'}], [], 'admin')
    try:
      assert_app_dependency_in_range(app, 'bar', upper='1.0')
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

  def test_assert_dependency_in_range_11(self):
    app = Application('foo', '1.0', [{'name' : 'bar', 'version' : '1.5'}], [], 'admin')
    try:
      assert_app_dependency_in_range(app, 'bar', lower='1.0', upper='2.0',
        exclude_upper=True, exclude_lower=True)
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

  def test_assert_dependency_in_range_12(self):
    app = Application('foo', '2.0', [{'name' : 'foo', 'version' : '1.0'}], [], 'admin')
    try:
      assert_app_dependency_in_range(app, 'foo', upper='1.0')
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

  def test_assert_dependency_in_range_13(self):
    app = Application('foo', '2.0', [{'name' : 'foo', 'version' : '1.5'}], [], 'admin')
    try:
      assert_app_dependency_in_range(app, 'foo', upper='1.0')
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_dependency_in_range_14(self):
    app = Application('foo', '2.0', [{'name' : 'bar', 'version' : '1.0'}], [], 'admin')
    try:
      assert_app_dependency_in_range(app, 'foo', upper='1.0')
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_not_dependency_1(self):
    app = Application('foo', '1.0', [], [], 'admin')
    try:
      assert_not_app_dependency(app, 'bar', '1.0')
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

  def test_assert_not_dependency_2(self):
    app = Application('foo', '1.0', [{'name' : 'bar', 'version' : '1.0'}], [], 'admin')
    try:
      assert_not_app_dependency(app, 'bar', '1.0')
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_not_dependency_3(self):
    app = Application('foo', '1.0', [], [], 'admin')
    try:
      assert_not_app_dependency(app, 'bar')
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

  def test_assert_not_dependency_4(self):
    app = Application('foo', '1.0', [{'name' : 'bar', 'version' : '1.0'}], [], 'admin')
    try:
      assert_not_app_dependency(app, 'bar')
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_not_dependency_5(self):
    app = Application('foo', '1.0', [{'name' : 'foo', 'version' : '1.0'}], [], 'admin')
    try:
      assert_not_app_dependency(app, 'foo')
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_not_dependency_6(self):
    app = Application('foo', '1.0', [{'name' : 'bar', 'version' : '1.0'}], [], 'admin')
    try:
      assert_not_app_dependency(app, 'foo')
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

  def test_assert_true(self):
    try:
      assert_true(True)
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

    try:
      assert_true(1)
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

    try:
      assert_true('not null')
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

    try:
      assert_true(False)
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_assert_false(self):
    try:
      assert_false(False)
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

    try:
      assert_false(0)
    except EagerPolicyAssertionException as ex:
      self.fail('Assertion threw exception')

    try:
      assert_false(True)
      self.fail('Assertion did not throw exception')
    except EagerPolicyAssertionException as ex:
      pass

  def test_version_comparison(self):
    self.assertTrue(compare_versions('1.0', '1.0') is 0)
    self.assertTrue(compare_versions('2.0', '1.0') > 0)
    self.assertTrue(compare_versions('1.0', '2.0') < 0)

    self.assertTrue(compare_versions('1.0.0', '1.0.0') is 0)
    self.assertTrue(compare_versions('2.0.0', '1.0.0') > 0)
    self.assertTrue(compare_versions('1.0.0', '2.0.0') < 0)

    self.assertTrue(compare_versions('2.2', '2.1') > 0)
    self.assertTrue(compare_versions('2.2', '2.3') < 0)

    self.assertTrue(compare_versions('1.0-rc1', '1.0-rc2') < 0)
    self.assertTrue(compare_versions('1.0', '1.0-rc1') > 0)

    self.assertTrue(compare_versions('2.0.0-alpha', '2.0.0-beta') < 0)
    self.assertTrue(compare_versions('2.0.0-beta', '2.0.0-alpha') > 0)

  def test_none_assertions(self):
    try:
      assert_true(None)
      self.fail("None was asserted True")
    except EagerPolicyAssertionException as ex:
      pass

    try:
      assert_false(None)
    except EagerPolicyAssertionException as ex:
      self.fail("None was not asserted False")
