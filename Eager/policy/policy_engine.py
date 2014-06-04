import os
import re
import sys
from policy.models import Policy, Application, API
from utils import utils
from threading import Lock

class PolicyEngine:

  POLICY_STORE_DIR = 'policystore'

  def __init__(self):
    parent_dir = os.path.dirname(os.path.realpath(sys.argv[0]))
    self.policy_store_dir = os.path.join(parent_dir, self.POLICY_STORE_DIR)
    self.active_policies = []
    self.inactive_policies = []
    self.lock = Lock()

    if os.path.exists(self.policy_store_dir):
      for policy_file in os.listdir(self.policy_store_dir):
        if policy_file.endswith('.a.py'):
          full_path = os.path.join(self.policy_store_dir, policy_file)
          try:
            policy = Policy(full_path)
            self.active_policies.append(policy)
          except Exception as ex:
            utils.log("Error while loading policy '{0}': {1}".format(policy_file, str(ex)))
        elif policy_file.endswith('.i.py'):
          full_path = os.path.join(self.policy_store_dir, policy_file)
          try:
            policy = Policy(full_path)
            self.inactive_policies.append(policy)
          except Exception as ex:
            utils.log("Error while loading policy '{0}': {1}".format(policy_file, str(ex)))

    if self.active_policies:
      msg_suffix = '1 policy'
      if len(self.active_policies) > 1:
        msg_suffix = '{0} policies'.format(len(self.active_policies))
      utils.log("Initialized policy engine with {0}.".format(msg_suffix))
    else:
      utils.log("No active policies found.")

  def __run_policy_enforcement(self, name, version, dependencies, api_list, owner):
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

  def __add_policy(self, name, content, active):
    regex = re.compile("^[a-zA-Z0-9_]+$")
    if not regex.match(name):
      return False, 'Invalid policy name: Only letters, digits and underscores are allowed'

    reg_name = re.compile(name + '\\.([ai])\\.py')
    for item in os.listdir(self.policy_store_dir):
      if reg_name.match(item):
       return False, 'Policy {0} already exists'.format(name)

    if active:
      file_path = os.path.join(self.policy_store_dir, name + '.a.py')
    else:
      file_path = os.path.join(self.policy_store_dir, name + '.i.py')

    file_handle = open(file_path, 'w')
    file_handle.write(content)
    file_handle.flush()
    file_handle.close()
    try:
      new_policy = Policy(file_path)
      if active:
        self.active_policies.append(new_policy)
      else:
        self.inactive_policies.append(new_policy)
      return True, None
    except Exception as ex:
      os.remove(file_path)
      return False, 'Error while parsing policy: {0}'.format(ex.message)

  def __remove_policy(self, name):
    reg_name = re.compile(name + '\\.([ai])\\.py')
    for item in os.listdir(self.policy_store_dir):
      match = reg_name.match(item)
      if match:
        break;

    if not match:
      return False, "Policy {0} is not found!".format(name)

    path = os.path.join(self.policy_store_dir, name + '.' + match.groups()[0] + '.py')
    os.remove(path)
    if match.groups()[0] == 'a':
      for p in self.active_policies:
        if p.name == name:
          self.active_policies.remove(p)
    else:
      for p in self.inactive_policies:
        if p.name == name:
          self.inactive_policies.remove(p)
    return True, "Policy removed successfully"

  # Enable an inactive policy, this policy must exist in the policy store
  def __enable_policy(self, name):
    path = os.path.join(self.policy_store_dir, name + '.i.py')
    if not os.path.exists(path):
      return False, "Error while enabling policy: {0}, no such inactive policy!".format(name)

    for p in self.inactive_policies:
      if p.name == name:
        try:
          os.rename(path, os.path.join(self.policy_store_dir, name + '.a.py'))
        except Exception as ex:
          return False, "Erro while enabling policy: {0}, {1}".format(name, ex.message)
        
        self.active_policies.append(p)
        self.inactive_policies.remove(p)
        return True, None
    # This should never happen:
    return False, "Error while enabling policy: {0}, policy engine is not consistent!".format(name)


  # Disable an active policy, this policy must exist in the policy store
  def __disable_policy(self, name):
    path = os.path.join(self.policy_store_dir, name + '.a.py')
    if not os.path.exists(path):
      return False, "Error while disabling policy: {0}, no such active policy!".format(name)

    for p in self.active_policies:
      if p.name == name:
        try:
          os.rename(path, os.path.join(self.policy_store_dir, name + '.i.py'))
        except Exception as ex:
          return False, "Error while disabling policy: {0}, {1}".format(name, ex.message)
        self.inactive_policies.append(p)
        self.active_policies.remove(p)
        return True, None

    #This should never happen:
    return False, "Error while disabling policy: {0}, policy engine is not consistent!".format(name)

  def __list_policy(self, status):
    if status == 'active':
      return [policy.name for policy in self.active_policies]

    if status == "inactive":
      return [policy.name  for policy in self.inactive_policies]

    if status == "all":
      return [policy.name for policy in self.active_policies + self.inactive_policies]

  def __info_policy(self, name):
    for p in self.active_policies + self.inactive_policies:
      if p.name == name:
        return (p.name, p.source_code)
    return None

  # Wrapper methods, added mutex
  def run_policy_enforcement(self, name, version, dependencies, api_list, owner):
    self.lock.acquire()
    res = self.__run_policy_enforcement(name, version, dependencies, api_list, owner)
    self.lock.release()
    return res

  def add_policy(self, name, content, active):
    self.lock.acquire()
    res = self.__add_policy(name, content, active)
    self.lock.release()
    return res

  def remove_policy(self, name):
    self.lock.acquire()
    res = self.__remove_policy(name)
    self.lock.release()
    return res

  def enable_policy(self, name):
    self.lock.acquire()
    res = self.__enable_policy(name)
    self.lock.release()
    return res

  def disable_policy(self, name):
    self.lock.acquire()
    res = self.__disable_policy(name)
    self.lock.release()
    return res

  def list_policy(self, status):
    self.lock.acquire()
    res = self.__list_policy(status)
    self.lock.release()
    utils.log(res[0])
    return res

  def info_policy(self, name):
    self.lock.acquire()
    res = self.__info_policy(name)
    self.lock.release()
    return res
