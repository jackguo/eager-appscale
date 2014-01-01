import json
import os
import sys
from apimgt import swagger
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
  REASON_BAD_SECRET = 'Bad secret'
  REASON_ALIVE = 'Service alive'
  REASON_API_VALIDATION_SUCCESS = 'API validated successfully'
  REASON_API_VALIDATION_FAILED = 'API validation failed'
  REASON_BAD_API_METADATA = 'API contains wrong or invalid metadata'
  REASON_AMBIGUOUS_API_NAME = 'API name is too similar to some names already in use'
  REASON_API_PUBLISH_SUCCESS = 'API published successfully'
  REASON_API_ALREADY_PUBLISHED = 'API already published'
  REASON_API_SPEC_UPDATE_FAILED = 'Failed to update API specification'
  REASON_BAD_API_DEPENDENCIES = 'Bad API dependencies'
  REASON_DEPENDENCY_RECORDING_FAILED = 'Failed to record API dependencies'
  REASON_BAD_API_SPEC = 'Bad API specification'

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

    spec_valid, spec_errors = swagger.validate_swagger_description(specification)
    if not spec_valid:
      detail = { 'detail' : spec_errors }
      return self.__generate_response(False, self.REASON_BAD_API_SPEC, detail)

    if dependencies and not self.adaptor.validate_api_dependencies(name, version, dependencies):
      detail = { 'detail' : 'One or more declared dependencies do not exist' }
      return self.__generate_response(False, self.REASON_BAD_API_DEPENDENCIES, detail)

    if self.adaptor.is_api_available(name, version):
      passed, reason, message = self.__invoke_api_validations(name, version, specification)
    else:
      passed, reason, message = self.__handle_new_api(name, version, specification)

    if not passed:
      if message:
        return self.__generate_response(False, reason, { 'detail' : message })
      else:
        return self.__generate_response(False, reason)

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

  def __invoke_api_validations(self, name, version, specification):
    utils.log("Validating API = {0}; Version = {1}".format(name, version))
    validation_info = self.adaptor.get_validation_info(name, version)
    val_status, val_message = self.__check_dependencies(specification, validation_info)
    if val_status:
      if self.adaptor.update_api_specification(name, version, json.dumps(specification)):
        utils.log("API specification updated successfully for {0}-v{1}".format(name, version))
        return True, self.REASON_API_VALIDATION_SUCCESS, None
      else:
        utils.log("Failed to update API specification for {0}-v{1}".format(name, version))
        return False, self.REASON_API_SPEC_UPDATE_FAILED, None
    else:
      utils.log("API dependency validation failed for {0}-v{1}: {2}".format(name, version, val_message))
      return False, self.REASON_API_VALIDATION_FAILED, val_message

  def __handle_new_api(self, name, version, specification):
    utils.log("API {0}-v{1} does not exist yet. Skipping dependency validation".format(
      name, version))
    context = '/' + name.lower()
    api_list = self.adaptor.get_api_list_with_context(context)
    if api_list:
      for api_info in api_list:
        if api_info.name != name:
          message = 'API name is too similar to: {0}'.format(api_info.name)
          return False, self.REASON_AMBIGUOUS_API_NAME, message
      utils.log("Context {0} is available for use".format(context))
    else:
      utils.log("Context {0} is not taken by any other API".format(context))

    if self.adaptor.create_api(name, version, json.dumps(specification)):
      utils.log("Successfully registered the API {0}-v{1}".format(name, version))
    else:
      utils.log("API {0}-v{1} is already registered".format(name, version))

    return True, self.REASON_API_VALIDATION_SUCCESS, None

  def __check_dependencies(self, specification, validation_info):
    operations = set()
    for dependent in validation_info.dependents:
      ops = dependent.operations
      for op in ops:
        operations.add(op)
    api_compatible, errors = swagger.is_api_compatible(validation_info.specification,
      specification, list(operations))
    if api_compatible:
      return True, 'api validated successfully'
    else:
      return False, errors
