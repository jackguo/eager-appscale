import os
from policy.assertions import *
from policy.policy_language import validate_policy, get_policy_description
from utils import utils

class API(tuple):
  __slots__ = []

  def __new__(cls, name, version):
    return tuple.__new__(cls, (name, version))

  @property
  def name(self):
    return tuple.__getitem__(self, 0)

  @property
  def version(self):
    return tuple.__getitem__(self, 1)

class Application(tuple):
  __slots__ = []

  def __new__(cls, name, version, dependencies, api_list, owner):
    return tuple.__new__(cls, (name, version, dependencies, api_list, owner))

  @property
  def name(self):
    return tuple.__getitem__(self, 0)

  @property
  def version(self):
    return tuple.__getitem__(self, 1)

  @property
  def dependencies(self):
    return tuple.__getitem__(self, 2)

  @property
  def api_list(self):
    return tuple.__getitem__(self, 3)

  @property
  def owner(self):
    return tuple.__getitem__(self, 4)

  def __getitem__(self, item):
    raise TypeError

class Policy:
  def __init__(self, policy_file):
    self.policy_file = policy_file
    self.name = self.__get_policy_name()
    self.source_code = self.__get_policy_content()
    self.description = get_policy_description(self.source_code)
    validate_policy(self.source_code)

  def __get_policy_name(self):
    base_name = os.path.basename(self.policy_file)
    return basename.split('.')[0]
#    return os.path.splitext(base_name)[0]

  def __get_policy_content(self):
    file_handle = open(self.policy_file, 'r')
    source_code = file_handle.read()
    file_handle.close()
    return source_code

  def evaluate(self, app, errors):
    globals_map = globals().copy()
    globals_map['app'] = app
    globals_map['assert_app_dependency'] = assert_app_dependency
    globals_map['assert_not_app_dependency'] = assert_not_app_dependency
    globals_map['assert_app_dependency_in_range'] = assert_app_dependency_in_range
    globals_map['assert_true'] = assert_true
    globals_map['assert_false'] = assert_false
    globals_map['compare_versions'] = compare_versions
    try:
      exec(self.source_code, globals_map, {})
    except EagerPolicyAssertionException as ex:
      errors.append('[{0}] {1}'.format(self.name, ex))
    except Exception as ex:
      utils.log('[{0}] Unexpected policy exception: {1}'.format(self.name, ex))
