import ast
import os
from policy.assertions import *
from policy.policy_language import validate_policy_content
from utils import utils

class API(tuple):
  __slots__ = []

  def __new__(cls, name, version, dependencies):
    return tuple.__new__(cls, (name, version, dependencies))

  @property
  def name(self):
    return tuple.__getitem__(self, 0)

  @property
  def version(self):
    return tuple.__getitem__(self, 1)

  @property
  def dependencies(self):
    return tuple.__getitem__(self, 2)

  def __getitem__(self, item):
    raise TypeError

class Policy:
  def __init__(self, policy_file):
    self.policy_file = policy_file
    self.name = self.__get_policy_name()
    code = self.__get_policy_ast()
    self.description = ast.get_docstring(code)
    validate_policy_content(code)

  def __get_policy_name(self):
    base_name = os.path.basename(self.policy_file)
    return os.path.splitext(base_name)[0]

  def __get_policy_ast(self):
    file_handle = open(self.policy_file, 'r')
    source_code = file_handle.read()
    file_handle.close()
    return ast.parse(source_code, mode='exec')

  def evaluate(self, api, errors):
    try:
      validate_policy_content(self.__get_policy_ast())
    except Exception as ex:
      utils.log('[{0}] Unexpected policy exception: {1}'.format(self.name, ex))
      return

    globals_map = globals().copy()
    globals_map['api'] = api
    globals_map['assert_dependency'] = assert_dependency
    globals_map['assert_not_dependency'] = assert_not_dependency
    globals_map['assert_dependency_in_range'] = assert_dependency_in_range
    globals_map['assert_true'] = assert_true
    globals_map['assert_false'] = assert_false
    try:
      execfile(self.policy_file, globals_map, {})
    except EagerPolicyAssertionException as ex:
      errors.append('[{0}] {1}'.format(self.name, ex))
    except Exception as ex:
      utils.log('[{0}] Unexpected policy exception: {1}'.format(self.name, ex))
