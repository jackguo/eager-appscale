from apimgt.wso2am_14 import WSO2APIManager14Adaptor

def get_adaptor(conf):
  provider_name = conf['api_manager']['provider']
  if provider_name == 'wso2am1.4':
    return WSO2APIManager14Adaptor(conf['api_manager'])
  else:
    raise Exception('Unknown API Manager provider: {0}'.format(provider_name))