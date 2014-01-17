import os
from policy.models import Policy
from policy.policy_engine import PolicyEngine

try:
  from unittest import TestCase
except ImportError:
  from unittest.case import TestCase

class TestPolicyEngine(TestCase):

  def __add_policy(self, name, engine):
    current_dir = os.path.dirname(os.path.abspath(__file__))
    full_path = os.path.join(current_dir, 'samples', name + '.py')
    policy = Policy(full_path)
    engine.active_policies.append(policy)

  def test_policy_engine_1(self):
    engine = PolicyEngine()
    passed, message = engine.run_policy_enforcement('Foo', '1.0', [], 'admin@test.com')
    self.assertTrue(passed)

  def test_policy_engine_2(self):
    engine = PolicyEngine()
    self.__add_policy('policy1', engine)
    passed, message = engine.run_policy_enforcement('MyAPI', '1.0', [], 'admin@test.com')
    self.assertFalse(passed)
    self.assertEqual("[policy1] Required dependency 'Foo-v1.0' not used", message)

  def test_policy_engine_3(self):
    engine = PolicyEngine()
    self.__add_policy('policy1', engine)
    dependencies = [{'name' : 'Foo', 'version' : '1.0'}]
    passed, message = engine.run_policy_enforcement('MyAPI', '1.0', dependencies, 'admin@test.com')
    self.assertTrue(passed)

  def test_policy_engine_4(self):
    engine = PolicyEngine()
    self.__add_policy('policy4', engine)
    passed, message = engine.run_policy_enforcement('MyAPI', '1.0', [], 'admin@test.com')
    self.assertTrue(passed)

  def test_policy_engine_5(self):
    engine = PolicyEngine()
    self.__add_policy('policy5', engine)
    dependencies = [{'name' : 'Foo', 'version' : '1.0'},{'name' : 'Bar', 'version' : '1.1'}]
    passed, message = engine.run_policy_enforcement('MyAPI', '1.0', dependencies, 'admin@test.com')
    self.assertFalse(passed)
    self.assertEquals('[policy5] All dependencies should be v1.0', message)