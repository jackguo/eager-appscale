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

  def __init__(self):
    self.secret = utils.get_secret()

  def ping(self, secret):
    if self.secret != secret:
      return self.__generate_response(False, self.REASON_BAD_SECRET)
    else:
      return self.__generate_response(True, self.REASON_ALIVE)

  def validate_api_for_deployment(self, secret, api):
    if self.secret != secret:
      return self.__generate_response(False, self.REASON_BAD_SECRET)
    # TODO: Perform the actual validation
    utils.log("Validating API = {0}; Version = {1}".format(api['name'], api['version']))
    return self.__generate_response(True, self.REASON_API_VALIDATION_SUCCESS)

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
