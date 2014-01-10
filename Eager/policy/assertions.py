class EagerPolicyAssertionException(Exception):
  pass

def assert_dependency(api, dep_name, dep_version=None):
  if is_api_equal(api.name, api.version, dep_name, dep_version):
    return
  if api.dependencies:
    for dependency in api.dependencies:
      if is_api_equal(dependency['name'], dependency['version'], dep_name, dep_version):
        return
  raise EagerPolicyAssertionException('Required dependency {0}-v{1} not used'.format(dep_name, dep_version))

def is_api_equal(name1, version1, name2, version2):
  if version2 is not None:
    return name1 == name2 and version1 == version2
  else:
    return name1 == name2

