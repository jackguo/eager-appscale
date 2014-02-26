from M2Crypto import SSL
from utils import utils
from eager import Eager
import os
import SOAPpy

__author__ = 'hiranya'
__email__ = 'hiranya@cs.ucsb.edu'

class EagerService:

  # Default bind address for EAGER service
  DEFAULT_HOST = '0.0.0.0'

  # Default port number for EAGER service
  DEFAULT_PORT = 18444

  APPSCALE_DIR = '/etc/appscale/'

  def __init__(self, host=DEFAULT_HOST, port=DEFAULT_PORT, ssl=True):
    """
    Initialize a new instance of the EAGER service.

    Args:
      host  Hostname to which the service should bind (Optional). Defaults
            to 0.0.0.0.
      port  Port of the service (Optional). Default to 18444.
      ssl   True if SSL should be engaged or False otherwise (Optional).
            Defaults to True. When engaged, this implementation expects
            to find the necessary SSL certificates in the /etc/appscale/certs
            directory.
    """
    self.host = host
    self.port = port

    secret = None
    while True:
      try:
        secret = utils.get_secret(self.APPSCALE_DIR + 'secret.key')
        break
      except Exception:
        utils.log('Waiting for the secret key to become available')
        utils.sleep(5)
    utils.log('Found the secret set to: {0}'.format(secret))

    SOAPpy.Config.simplify_objects = True

    if ssl:
      utils.log('Checking for the certificate and private key')
      cert = self.APPSCALE_DIR + 'certs/mycert.pem'
      key = self.APPSCALE_DIR + 'certs/mykey.pem'
      while True:
        if os.path.exists(cert) and os.path.exists(key):
          break
        else:
          utils.log('Waiting for certificates')
          utils.sleep(5)

      ssl_context = SSL.Context()
      ssl_context.load_cert(cert, key)
      self.server = SOAPpy.SOAPServer((host, port), ssl_context=ssl_context)
    else:
      self.server = SOAPpy.SOAPServer((host, port))

    e = Eager()
    self.server.registerFunction(e.ping)
    self.server.registerFunction(e.validate_application_for_deployment)
    self.server.registerFunction(e.publish_api_list)
    self.started = False

  def start(self):
    """
    Start the EAGER service. This method blocks
    as long as the service is alive. The caller should handle the
    threading requirements
    """
    if self.started:
      utils.log('Warning - Start called on already running server')
    else:
      utils.log('Starting EAGER service on port: ' + str(self.port))
      self.started = True
      while self.started:
        self.server.serve_forever()

  def stop(self):
    """
    Stop the EAGER service.
    """
    if self.started:
      utils.log('Stopping EAGER service')
      self.started = False
      self.server.shutdown()
    else:
      utils.log('Warning - Stop called on already stopped server')

if __name__ == '__main__':
  service = EagerService()
  service.start()
