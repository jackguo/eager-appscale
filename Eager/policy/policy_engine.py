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
    self.inactive_policise = []
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
	    self.active_policies.append(policy)
	  except Exception as ex:
	    utils.log("Error while loading policy '{0}': {1}".format(policy_file, str(ex))

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

  def __add_policy(self, name, content, active=True):
    regex = re.compile("^[a-zA-Z0-9_]+$")
    if not regex.match(name):
      return False, 'Invalid policy name: Only letters, digits and underscores are allowed'

    if active:
      file_path = os.path.join(self.policy_store_dir, name + '.a.py')
    else:
      file_path = os.path.join(self.policy_store_dir, name + '.i.py')

    if os.path.exists(file_path):
      return False, 'Policy {0} already exists'.format(name)

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
	return False

    path = os.path.join(self.policy_store_dir, name + '.' + match.group(0) + '.py')
    os.remove(path)
    if match.group(0) is 'a':
	for p in self.active_policies:
		if p.name is name:
			self.active_policies.remove(p)
    else
	for p in self.inactive_policies:
		if p.name is name:
			self.inactive_policies.remove(p)
    return True

  # Enable an inactive policy, this policy must exist in the policy store
  def __enable_policy(self, name):
    path = os.path.join(self.policy_store_dir, name + '.i.py')
    if not os.path.exists(path):
	return False, "Error while enabling policy: {0}, no such inactive policy!\n".format(name)

    for p in self.inactive_polices:
	if p.name is name:
		try:
		  os.path.rename(path, os.path.join(self.policy_store_dir, name + '.a.py')
		except Exception as ex:
		  return False, "Erro while enabling policy: {0}, {1}".format(name, ex.message)
		
		self.active_polices.append(p)
		self.inactive_polices.remove(p)
		return True, None
    # This should never happen:
    return False, "Error while enabling policy: {0}, policy engine is not consistent!".format(name)


  # Disable an active policy, this policy must exist in the policy store
  def __disable_policy(self, name):
    path = os.path.join(self.policy_store_dir, name + '.a.py')
    if not os.path.exists(path):
	return False, "Error while disabling policy: {0}, no such active policy!".format(name)

    for p in self.active_policies:
	if p.name is name:
		try:
		  os.path.rename(path, os.path.join(self.policy_store_dir, name + '.i.py')
		except Exception as ex:
		  return False, "Error while disabling policy: {0}, {1}".format(name, ex.message)

		self.inactive_policies.append(p)
		self.active_policies.remove(p)
		return True, None

    #This should never happen:
    return False, "Error while disabling policy: {0}, policy engine is not consistent!".format(name)

  def __list_policy(self, status):
    if status is 'active':
      return [(policy.name, policy.discription) for policy in self.active_policies]

    if status is "inactive":
      return [(policy.name, policy.discription) for policy in self.inactive_policies]

    if status is "all":
      return [(policy.name, policy.discription) for policy in self.active_policies + self.inactive_polices]

  def __info_policy(self, name):
    for p in self.active_policies + self.inactive_polices:
	if p.name == name:
		return (p.name, p.source_code)
    return None

  # Wrapper methods, added mutex
  def run_policy_enforcement(self, name, version, dependencies, api_list, owner):
	self.lock.acquire()
	re = self.___run_policy_enforcement(self, name, version, dependencies, api_list, owner)
	self.lock.release()
	return re

  def add_policy(self, name, content, active=True):
	self.lock.acquire()
	re = self.__add_policy(self, name, content, active=True)
	self.lock.release()
	return re

  def remove_policy(self, name):
	self.lock.acquire()
	re = self.__remove_policy(self, name)
	self.lock.release()
	return re

  def enable_policy(self, name):
	self.lock.acquire()
	re = self.__enable_policy(self, name)
	self.lock.release()
	return re

  def disable_policy(self, name):
	self.lock.acquire()
	re = self.__disable_policy(self, name)
	self.lock.release()
	return re

  def list_policy(self, status):
	self.lock.acquire()
	re = self.__list_policy(self, status)
	self.lock.release()
	return re

  def info_policy(self, name):
	self.lock.acquire()
	re = self.__info_policy(self, name)
	self.lock.release()
	return re
