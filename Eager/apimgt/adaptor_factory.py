from apimgt.wso2am_14 import WSO2APIManager14Adaptor

CONF_AM = 'api_manager'
CONF_AM_PROVIDER = 'provider'

def get_adaptor(conf):
  provider_name = conf[CONF_AM][CONF_AM_PROVIDER]
  if provider_name == 'wso2am1.4':
    return WSO2APIManager14Adaptor(conf[CONF_AM])
  else:
    raise Exception('Unknown API Manager provider: {0}'.format(provider_name))