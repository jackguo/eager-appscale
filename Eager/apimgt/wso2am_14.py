import logging
from suds.client import Client
from suds.transport.http import HttpAuthenticated
from apimgt.adaptor import APIManagerAdaptor, APIInfo, DependencyInfo, ValidationInfo

__author__ = 'hiranya'
__email__ = 'hiranya@cs.ucsb.edu'

class WSO2APIManager14Adaptor(APIManagerAdaptor):
  def __init__(self, conf):
    self.url = 'https://{0}:{1}/services/EagerAdmin'.format(conf['host'],
      conf['port'])
    self.__init_service_client(conf)

  def __init_service_client(self, conf):
    if conf.get('debug'):
      logging.basicConfig(level=logging.INFO)
      logging.getLogger('suds.client').setLevel(logging.DEBUG)
    transport = HttpAuthenticated(username=conf['user'], password=conf['password'])
    self.client = Client(self.url + '?wsdl', location=self.url,
      transport=transport, cache=None)

  def is_api_available(self, name, version):
    api = { 'name' : name, 'version' : version }
    return self.client.service.isAPIAvailable(api=api)

  def get_api_list_with_context(self, context):
    results = self.client.service.getAPIsWithContext(context)
    api_list = []
    for result in results:
      api_list.append(APIInfo(result['name'], result['version']))
    return api_list

  def create_api(self, name, version, specification):
    api = { 'name' : name, 'version' : version }
    return self.client.service.createAPI(api=api, specification=specification)


  def get_dependency_validation_info(self, name, version):
    api = { 'name' : name, 'version' : version }
    return self.client.service.getValidationInfo(api=api)

  def publish_api(self, name, version, url):
    api = { 'name' : name, 'version' : version }
    return self.client.service.publishAPI(api=api, url=url)

  def update_api_specification(self, name, version, specification):
    api = { 'name' : name, 'version' : version }
    return self.client.service.updateAPISpec(api=api, specification=specification)

  def get_validation_info(self, name, version):
    api = { 'name' : name, 'version' : version }
    result = self.client.service.getValidationInfo(api=api)
    dependents = []
    if hasattr(result, 'dependents'):
      for dependent in result.dependents:
        dep_name = dependent.name
        dep_version = dependent.version
        dep_operations = []
        for op in dependent.operations:
          dep_operations.append(op)
        d = DependencyInfo(dep_name, dep_version, dep_operations)
        dependents.append(d)
    return ValidationInfo(result.specification, dependents)

  def validate_api_dependencies(self, name, version, dependencies):
    api = { 'name' : name, 'version' : version }
    return self.client.service.validateDependencies(api=api, dependencies=dependencies)

  def record_api_dependencies(self, name, version, dependencies):
    api = { 'name' : name, 'version' : version }
    return self.client.service.recordDependencies(api=api, dependencies=dependencies)