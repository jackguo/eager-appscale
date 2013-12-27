import json
import os
import sys
from utils import utils

__author__ = 'hiranya'
__email__ = 'hiranya@appscale.com'

class Eager:
  """
  Eager class is the main entry point to the AppScale Eager
  implementation. An instance of this class can be used to
  perform EAGER policy enforcement on web APIs at the deployment
  time of AppScale apps.
  """

  # Default reasons which might be returned by this module
  REASON_BAD_SECRET = 'bad secret'
  REASON_ALIVE = 'service alive'
  REASON_API_VALIDATION_SUCCESS = 'api validated successfully'
  REASON_BAD_API_METADATA = 'api contains wrong or invalid metadata'
  REASON_AMBIGUOUS_API_NAME = 'api name is too similar to some names already in use'
  REASON_API_PUBLISH_SUCCESS = 'api published successfully'
  REASON_API_PUBLISH_FAILURE = 'api not published'

  CONFIG_FILE = 'eager.yaml'

  def __init__(self):
    self.secret = utils.get_secret()
    parent_dir = os.path.dirname(os.path.realpath(sys.argv[0]))
    config_file = os.path.join(parent_dir, self.CONFIG_FILE)
    self.adaptor = utils.get_adaptor(config_file)

  def ping(self, secret):
    if self.secret != secret:
      return self.__generate_response(False, self.REASON_BAD_SECRET)
    else:
      return self.__generate_response(True, self.REASON_ALIVE)

  def validate_api_for_deployment(self, secret, api):
    if self.secret != secret:
      return self.__generate_response(False, self.REASON_BAD_SECRET)

    name = api['name']
    version = api['version']
    specification = api['specification']

    if not self.__is_api_name_valid(name):
      detail = { 'detail' : 'API name contains invalid characters' }
      return self.__generate_response(False, self.REASON_BAD_API_METADATA, detail)

    if self.adaptor.is_api_available(name, version):
      utils.log("Validating API = {0}; Version = {1}".format(name, version))
      # TODO: Get ValidationInfo and run dependency checker algorithm
      # TODO: If successful, update the API spec in API Manager
    else:
      utils.log("API {0}-v{1} does not exist yet. Skipping dependency validation".format(
        name, version))
      context = '/' + name.lower()
      api_list = self.adaptor.get_api_list_with_context(context)
      if api_list:
        for api_info in api_list:
          if api_info.name != name:
            detail = { 'detail' : 'API name is too similar to: {0}'.format(api_info.name) }
            self.__generate_response(False, self.REASON_AMBIGUOUS_API_NAME, detail)
        utils.log("Context {0} is available for use".format(context))
      else:
        utils.log("Context {0} is not taken by any other API".format(context))
        if self.adaptor.create_api(name, version, json.dumps(specification)):
          utils.log("Successfully registered the API {0}-v{1}".format(name, version))
        else:
          utils.log("API {0}-v{1} is already registered".format(name, version))

    # TODO: Record API dependencies
    return self.__generate_response(True, self.REASON_API_VALIDATION_SUCCESS)

  def publish_api(self, secret, api, url):
    if self.secret != secret:
      return self.__generate_response(False, self.REASON_BAD_SECRET)

    name = api['name']
    version = api['version']
    if self.adaptor.publish_api(name, version, url):
      utils.log("API {0}-v{1} published successfully".format(name, version))
      return self.__generate_response(True, self.REASON_API_PUBLISH_SUCCESS)
    else:
      detail = { 'detail' : 'API {0}-v{1} does not exist'.format(name, version) }
      return self.__generate_response(False, self.REASON_API_PUBLISH_FAILURE, detail)

  def __is_api_name_valid(self, name):
    for char in "'/ &+@%\"<>":
      if char in name:
        return False
    return True

  def __generate_response(self, status, msg, extra=None):
    """
    Generate an EAGER service response

    Args:
      status  A boolean value indicating the status
      msg     A reason message (useful if this a failed operation)
      extra   Any extra fields to be included in the response (Optional)

    Returns:
      A dictionary containing the operation response
    """
    utils.log("Sending success = {0}, reason = {1}".format(status, msg))
    response = {'success': status, 'reason': msg}
    if extra is not None:
      for key, value in extra.items():
        response[key] = value
    return response
