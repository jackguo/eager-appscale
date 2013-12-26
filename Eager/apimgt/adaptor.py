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
