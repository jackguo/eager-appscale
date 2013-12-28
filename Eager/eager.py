import json
import os
import sys
from utils import utils

__author__ = 'hiranya'
__email__ = 'hiranya@cs.ucsb.edu'

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
  REASON_API_VALIDATION_FAILED = 'api validation failed'
  REASON_BAD_API_METADATA = 'api contains wrong or invalid metadata'
  REASON_AMBIGUOUS_API_NAME = 'api name is too similar to some names already in use'
  REASON_API_PUBLISH_SUCCESS = 'api published successfully'
  REASON_API_ALREADY_PUBLISHED = 'api already published'
  REASON_API_SPEC_UPDATE_FAILED = 'failed to update api specification'
  REASON_BAD_API_DEPENDENCIES = 'bad api dependencies'
  REASON_DEPENDENCY_RECORDING_FAILED = 'failed to record api dependencies'

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
    dependencies = api['dependencies']

    if not self.__is_api_name_valid(name):
      detail = { 'detail' : 'API name contains invalid characters' }
      return self.__generate_response(False, self.REASON_BAD_API_METADATA, detail)

    if dependencies and not self.adaptor.validate_api_dependencies(name, version, dependencies):
      detail = { 'detail' : 'One or more declared dependencies do not exist' }
      return self.__generate_response(False, self.REASON_BAD_API_DEPENDENCIES, detail)

    if self.adaptor.is_api_available(name, version):
      utils.log("Validating API = {0}; Version = {1}".format(name, version))
      validation_info = self.adaptor.get_validation_info(name, version)
      val_status, val_message = self.__check_dependencies(specification, validation_info)
      if val_status:
        if self.adaptor.update_api_specification(name, version, json.dumps(specification)):
          utils.log("API specification updated successfully for {0}-v{1}".format(name, version))
        else:
          msg = "Failed to update API specification for {0}-v{1}".format(name, version)
          utils.log(msg)
          detail = { 'detail' : msg }
          return self.__generate_response(False, self.REASON_API_SPEC_UPDATE_FAILED, detail)
      else:
        detail = { 'detail' : val_message }
        return self.__generate_response(False, self.REASON_API_VALIDATION_FAILED, detail)
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

    if not self.adaptor.record_api_dependencies(name, version, dependencies):
      utils.log("Failed to record dependencies for {0}-v{1}".format(name, version))
      return self.__generate_response(False, self.REASON_DEPENDENCY_RECORDING_FAILED)

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
      utils.log("API {0}-v{1} is already published".format(name, version))
      return self.__generate_response(True, self.REASON_API_ALREADY_PUBLISHED)

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
    response = {'success': status, 'reason': msg}
    if extra is not None:
      for key, value in extra.items():
        response[key] = value
    return response

  def __check_dependencies(self, specification, validation_info):
    utils.log("Current specification:" + str(specification))
    utils.log("Retrieved specification: " + str(validation_info.specification))
    # TODO: Run dependency checker + other policy enforcement logic
    return True, 'api validated successfully'
