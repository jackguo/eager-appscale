from policy.models import API

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