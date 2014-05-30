import json
import os
import sys
from apimgt import swagger
from policy import policy_engine
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
  REASON_API_POLICY_VIOLATION = 'API violates one or more policies'
  REASON_BAD_API_METADATA = 'API contains wrong or invalid metadata'
  REASON_API_PUBLISH_SUCCESS = 'APIs published successfully'
  REASON_API_PUBLISH_FAILED = 'Failed to publish one or more APIs'
  REASON_BAD_DEPENDENCIES = 'Bad dependencies'
  REASON_DEPENDENCY_RECORDING_FAILED = 'Failed to record API dependencies'

  CONFIG_FILE = 'eager.yaml'

  def __init__(self):
    self.secret = utils.get_secret()
    parent_dir = os.path.dirname(os.path.realpath(sys.argv[0]))
    config_file = os.path.join(parent_dir, self.CONFIG_FILE)
    self.adaptor = utils.get_adaptor(config_file)
    self.policy_engine = policy_engine.PolicyEngine()

  def ping(self, secret):
    if self.secret != secret:
      return self.__generate_response(False, self.REASON_BAD_SECRET)
    else:
      return self.__generate_response(True, self.REASON_ALIVE)

  def validate_application_for_deployment(self, secret, app):
    if self.secret != secret:
      return self.__generate_response(False, self.REASON_BAD_SECRET)

    name = app['name']
    version = app['version']
    dependencies = app['dependencies']
    api_list = app['api_list']
    api_list_without_specs = self.__remove_specs(api_list)
    owner = app['owner']

    if dependencies:
      dep_invalid = self.adaptor.validate_application_dependencies(name, version,
        api_list_without_specs, dependencies)
      if dep_invalid:
        detail = { 'detail' : dep_invalid }
        return self.__generate_response(False, self.REASON_BAD_DEPENDENCIES, detail)

    pre_validation_errors = []
    for api in api_list:
      api_name = api['name']
      api_spec = api['specification']
      if not self.__is_api_name_valid(api_name):
        pre_validation_errors.append('Invalid characters in API name: {0}'.format(api_name))
      spec_valid, spec_errors = swagger.validate_swagger_description(api_spec)
      if not spec_valid:
        pre_validation_errors.append(spec_errors)

    if pre_validation_errors:
      detail = { 'detail' : '|'.join(pre_validation_errors) }
      return self.__generate_response(False, self.REASON_BAD_API_METADATA, detail)

    passed, message = self.policy_engine.run_policy_enforcement(name, version, dependencies,
      api_list_without_specs, owner)
    if not passed:
      detail = { 'detail' : message }
      return self.__generate_response(False, self.REASON_API_POLICY_VIOLATION, detail)

    post_validation_errors = []
    for api in api_list:
      api_name = api['name']
      api_version = api['version']
      api_spec = api['specification']
      if self.adaptor.is_api_available(api_name, api_version):
        passed, message = self.__invoke_api_validations(api_name, api_version, api_spec)
      else:
        passed, message = self.__handle_new_api(api_name, api_version, api_spec)
      if not passed:
        post_validation_errors.append(message)

    if post_validation_errors:
      detail = { 'detail' : '|'.join(post_validation_errors)}
      return self.__generate_response(False, self.REASON_API_VALIDATION_FAILED, detail)

    if not self.adaptor.record_application_dependencies(name, version, api_list_without_specs, dependencies):
      utils.log("Failed to record dependencies for {0}-v{1}".format(name, version))
      return self.__generate_response(False, self.REASON_DEPENDENCY_RECORDING_FAILED)

    return self.__generate_response(True, self.REASON_API_VALIDATION_SUCCESS)

  def publish_api_list(self, secret, api_list, url):
    if self.secret != secret:
      return self.__generate_response(False, self.REASON_BAD_SECRET)

    temp_api_list = []
    for api in api_list:
      temp_api_list.append({ 'name' : api['name'], 'version' : api['version'] })

    success, message = self.adaptor.publish_api_list(temp_api_list, url)
    if success:
      if len(api_list) == 1:
        utils.log("API published successfully")
      else:
        utils.log("{0} APIs published successfully".format(len(api_list)))
      return self.__generate_response(True, self.REASON_API_PUBLISH_SUCCESS)
    else:
      utils.log("Failed to publish one or more APIs: {0}".format(message))
      detail = { 'detail' : message }
      return self.__generate_response(False, self.REASON_API_PUBLISH_FAILED, detail)

  def add_policy(self, secret, name, content, active):
    if self.secret != secret:
      return self.__generate_response(False, self.REASON_BAD_SECRET)

    return  self.policy_engine.add_policy(name, content, active)


  def __is_api_name_valid(self, name):
    for char in "'/ &+*@%\"<>!,":
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
        return True, None
      else:
        utils.log("Failed to update API specification for {0}-v{1}".format(name, version))
        return False, 'Failed to update API specification'
    else:
      utils.log("API dependency validation failed for {0}-v{1}: {2}".format(name, version, val_message))
      return False, val_message

  def __handle_new_api(self, name, version, specification):
    utils.log("API {0}-v{1} does not exist yet. Skipping dependency validation".format(
      name, version))
    context = '/' + name.lower()
    api_list = self.adaptor.get_api_list_with_context(context)
    if api_list:
      for api_info in api_list:
        if api_info.name != name:
          return False, 'API name is too similar to: {0}'.format(api_info.name)
      utils.log("Context {0} is available for use".format(context))
    else:
      utils.log("Context {0} is not taken by any other API".format(context))

    if self.adaptor.create_api(name, version, json.dumps(specification)):
      utils.log("Successfully registered the API {0}-v{1}".format(name, version))
    else:
      utils.log("API {0}-v{1} is already registered".format(name, version))

    return True, None

  def __check_dependencies(self, specification, validation_info):
    operations = set()
    for dependent in validation_info.dependents:
      if dependent.operations:
        operations.update(dependent.operations)
      else:
        # If any dependent has an empty operation list, we need to test
        # all operations for compatibility.
        operations.clear()
        break

    api_compatible, errors = swagger.is_api_compatible(validation_info.specification,
      specification, list(operations))
    if api_compatible:
      return True, 'api validated successfully'
    else:
      return False, errors

  def __remove_specs(self, api_list):
    result = []
    for api in api_list:
      result_api = {
        'name' : api['name'],
        'version' : api['version']
      }
      result.append(result_api)
    return result
