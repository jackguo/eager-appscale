import os
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
