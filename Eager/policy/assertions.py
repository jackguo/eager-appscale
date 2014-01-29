from pkg_resources import parse_version

class EagerPolicyAssertionException(Exception):
  pass

def assert_dependency(api, name, version=None):
  if is_api_equal(api.name, api.version, name, version):
    return
  if api.dependencies:
    for dependency in api.dependencies:
      if is_api_equal(dependency['name'], dependency['version'], name, version):
        return
  raise EagerPolicyAssertionException("Required dependency '{0}' not used".format(
    get_dependency_string(name, version)))

def assert_not_dependency(api, name, version=None):
  if api.dependencies:
    for dependency in api.dependencies:
      if is_api_equal(dependency['name'], dependency['version'], name, version):
        raise EagerPolicyAssertionException("Prohibited dependency '{0}' in use".format(
          get_dependency_string(name, version)))

def assert_dependency_in_range(api, name, lower=None, upper=None,
                               exclude_lower=False, exclude_upper=False):
  if lower is None and upper is None:
    assert_dependency(api, name)
  elif not api.dependencies:
    raise EagerPolicyAssertionException("Required dependency '{0}' not used".format(name))

  dep_found = False
  for dependency in api.dependencies:
    if name == dependency['name']:
      dep_found = True
      match_found = True
      version = dependency['version']
      if lower is not None:
        comp = compare_versions(lower, version)
        if exclude_lower and comp >= 0:
          match_found = False
        elif not exclude_lower and comp > 0:
          match_found = False

      if not match_found:
        continue

      if upper is not None:
        comp = compare_versions(upper, version)
        if exclude_upper and comp <= 0:
          match_found = False
        elif not exclude_upper and comp < 0:
          match_found = False

      if match_found:
        return
  if dep_found:
    range_str = get_version_range_string(lower, upper, exclude_lower, exclude_upper)
    raise EagerPolicyAssertionException("Version of required dependency '{0}' is not in " \
                                        "the range {1}".format(name, range_str))
  else:
    raise EagerPolicyAssertionException("Required dependency '{0}' not used".format(name))

def assert_true(condition, msg=None):
  if bool(condition) is False:
    raise EagerPolicyAssertionException(msg or "Condition was expected to be true, but evaluated to false")

def assert_false(condition, msg=None):
  if bool(condition) is True:
    raise EagerPolicyAssertionException(msg or "Condition was expected to be false, but evaluated to true")

def is_api_equal(name1, version1, name2, version2):
  if version2 is not None:
    return name1 == name2 and version1 == version2
  else:
    return name1 == name2

def compare_versions(version1, version2):
  v1 = parse_version(version1)
  v2 = parse_version(version2)
  if v1 > v2:
    return 1
  elif v1 < v2:
    return -1
  else:
    return 0

def get_version_range_string(version_lower, version_higher, ex_lower, ex_higher):
  output = ''
  if version_lower:
    if ex_lower:
      output += '(' + version_lower + ','
    else:
      output += '[' + version_lower + ','
  else:
    output += '(*,'

  if version_higher:
    if ex_higher:
      output += version_higher + ')'
    else:
      output += version_higher + ']'
  else:
    output += '*)'
  return output

def get_dependency_string(name, version):
  if version:
    return "{0}-v{1}".format(name, version)
  else:
    return name

