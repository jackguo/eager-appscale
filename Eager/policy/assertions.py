from utils import utils

class EagerPolicyAssertionException(Exception):
  pass

def assert_dependency(api, dep_name, dep_version=None):
  utils.log('{0}, {1}'.format(api, dep_name))
  if api.name == dep_name and api.version == dep_version:
    return
  if api.dependencies:
    for dependency in api.dependencies:
      if dependency['name'] == dep_name and dependency['version'] == dep_version:
        return
  raise EagerPolicyAssertionException('Required dependency {0}-v{1} not used'.format(dep_name, dep_version))

