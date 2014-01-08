from utils import utils

class EagerPolicyAssertionException(Exception):
  pass

def assert_dependency(api, dep_name, dep_version=None):
  utils.log('{0}, {1}'.format(api, dep_name))