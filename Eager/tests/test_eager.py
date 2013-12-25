from flexmock import flexmock
from utils import utils
from eager import *
try:
  from unittest import TestCase
except ImportError:
  from unittest.case import TestCase

__author__ = 'hiranya'
__email__ = 'hiranya@appscale.com'

class TestEager(TestCase):
  def setUp(self):
    flexmock(utils).should_receive('get_secret').and_return('secret')
    flexmock(utils).should_receive('get_adaptor').and_return(None)

  def tearDown(self):
    flexmock(utils).should_receive('get_secret').reset()
    flexmock(utils).should_receive('get_adaptor').reset()

  def test_initialize(self):
    e = Eager()
    self.assertEquals('secret', e.secret)

  def test_ping(self):
    e = Eager()

    result1 = e.ping('secret1')
    self.assertFalse(result1['success'])
    self.assertEquals(result1['reason'], Eager.REASON_BAD_SECRET)

    result2 = e.ping('secret')
    self.assertTrue(result2['success'])
    self.assertEquals(result2['reason'], Eager.REASON_ALIVE)
