import os
import sys
from policy.assertions import *
from policy.models import API

class PolicyEngine:

  POLICY_STORE_DIR = 'policystore'

  def __init__(self):
    parent_dir = os.path.dirname(os.path.realpath(sys.argv[0]))
    self.policy_store_dir = os.path.join(parent_dir, self.POLICY_STORE_DIR)
    self.active_policies = []
    if os.path.exists(self.policy_store_dir):
      for policy_file in os.listdir(self.policy_store_dir):
        if policy_file.endswith('.py'):
          self.active_policies.append(os.path.join(self.policy_store_dir, policy_file))

  def run_policy_enforcement(self, name, version, dependencies):
    if self.active_policies:
      api = API(name, version, dependencies)
      globals_map = globals().copy()
      globals_map['api'] = api
      globals_map['assert_dependency'] = assert_dependency
      errors = []
      for policy_file in self.active_policies:
        try:
          execfile(policy_file, globals_map)
        except EagerPolicyAssertionException as ex:
          errors.append('{0}: {1}'.format(os.path.splitext(policy_file)[0], ex.message))
      if errors:
        return False, '|'.join(errors)
    return True, None

