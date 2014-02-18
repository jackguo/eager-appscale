import os
import re
import sys
from policy.models import Policy, Application, API
from utils import utils

class PolicyEngine:

  POLICY_STORE_DIR = 'policystore'

  def __init__(self):
    parent_dir = os.path.dirname(os.path.realpath(sys.argv[0]))
    self.policy_store_dir = os.path.join(parent_dir, self.POLICY_STORE_DIR)
    self.active_policies = []
    if os.path.exists(self.policy_store_dir):
      for policy_file in os.listdir(self.policy_store_dir):
        if policy_file.endswith('.py'):
          full_path = os.path.join(self.policy_store_dir, policy_file)
          try:
            policy = Policy(full_path)
            self.active_policies.append(policy)
          except Exception as ex:
            utils.log("Error while loading policy '{0}': {1}".format(policy_file, str(ex)))

  def run_policy_enforcement(self, name, version, dependencies, api_list, owner):
    if self.active_policies:
      immutable_api_list = []
      for api in api_list:
        immutable_api_list.append(API(api['name'], api['version']))
      app = Application(name, version, dependencies, immutable_api_list, owner)
      errors = []
      for policy in self.active_policies:
        policy.evaluate(app, errors)
      if errors:
        return False, '|'.join(errors)
    return True, None

  def add_policy(self, name, content):
    regex = re.compile("^[a-zA-Z0-9_]+$")
    if not regex.match(name):
      return False, 'Invalid policy name: Only letters, digits and underscores are allowed'
    file_path = os.path.join(self.policy_store_dir, name + '.py')
    if os.path.exists(file_path):
      return False, 'Policy {0} already exists'.format(name)
    file_handle = open(file_path, 'w')
    file_handle.write(content)
    file_handle.flush()
    file_handle.close()
    try:
      new_policy = Policy(file_path)
      self.active_policies.append(new_policy)
      return True, None
    except Exception as ex:
      os.remove(file_path)
      return False, 'Error while parsing policy: {0}'.format(ex.message)

  def remove_policy(self, name):
    file_path = os.path.join(self.policy_store_dir, name + '.py')
    if os.path.exists(file_path):
      os.remove(file_path)
      for p in self.active_policies:
        if p.name == name:
          self.active_policies.remove(p)
          break
      return True
    else:
      return False
