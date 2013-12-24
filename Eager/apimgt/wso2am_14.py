import logging
from suds.client import Client
from suds.transport.http import HttpAuthenticated
from apimgt.adaptor import APIManagerAdaptor

class WSO2APIManager14Adaptor(APIManagerAdaptor):
  def __init__(self, conf):
    self.url = 'https://{0}:{1}/services/EagerAdmin'.format(conf['host'],
      conf['port'])
    self.user = conf['user']
    if conf.get('debug'):
      logging.basicConfig(level=logging.INFO)
      logging.getLogger('suds.client').setLevel(logging.DEBUG)

    transport = HttpAuthenticated(username=conf['user'], password=conf['password'])
    self.client = Client(self.url + '?wsdl', location=self.url,
      transport=transport, cache=None)

  def is_api_available(self, name, version):
    return self.client.service.isAPIAvailable(name=name,
      version=version, providerName=self.user)
