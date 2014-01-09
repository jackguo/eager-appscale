import ast
import os

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
    base_name = os.path.basename(policy_file)
    self.name = os.path.splitext(base_name)[0]
    file_handle = open(policy_file, 'r')
    source_code = file_handle.read()
    file_handle.close()
    code = ast.parse(source_code)
    self.description = ast.get_docstring(code)
    self.policy_file = policy_file

