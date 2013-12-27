class APIInfo:
  def __init__(self, name, version):
    self.name = name
    self.version = version

class APIManagerAdaptor(object):

  def is_api_available(self, name, version):
    raise NotImplementedError

  def get_api_list_with_context(self, context):
    raise NotImplementedError

  def create_api(self, name, version, specification):
    raise NotImplementedError

  def get_dependency_validation_info(self, name, version):
    raise NotImplementedError

  def publish_api(self, name, version, url):
    raise NotImplementedError
