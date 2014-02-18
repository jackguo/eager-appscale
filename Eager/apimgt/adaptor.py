import json

__author__ = 'hiranya'
__email__ = 'hiranya@cs.ucsb.edu'

class APIInfo:
  def __init__(self, name, version):
    self.name = name
    self.version = version

class DependencyInfo:
  def __init__(self, name, version, operations=[]):
    self.name = name
    self.version = version
    self.operations = operations

class ValidationInfo:
  def __init__(self, specification, dependents=[]):
    if not isinstance(specification, dict):
      self.specification = json.loads(str(specification))
    else:
      self.specification = specification
    self.dependents = dependents

class APIManagerAdaptor(object):

  def is_api_available(self, name, version):
    raise NotImplementedError

  def get_api_list_with_context(self, context):
    raise NotImplementedError

  def create_api(self, name, version, specification):
    raise NotImplementedError

  def publish_api(self, name, version, url):
    raise NotImplementedError

  def update_api_specification(self, name, version, specification):
    raise NotImplementedError

  def get_validation_info(self, name, version):
    raise NotImplementedError

  def validate_api_dependencies(self, name, version, dependencies):
    raise NotImplementedError

  def record_api_dependencies(self, name, version, dependencies):
    raise NotImplementedError
