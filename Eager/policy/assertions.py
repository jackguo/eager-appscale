from pkg_resources import parse_version

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

def assert_dependency_in_range(api, dep_name, dep_version_lower=None,
                                       dep_version_higher=None, dep_ex_lower=False,
                                       dep_ex_higher=False):
  if dep_version_lower is None and dep_version_higher is None:
    assert_dependency(api, dep_name)
  elif not api.dependencies:
    raise EagerPolicyAssertionException('Required dependency {0} not used'.format(dep_name))

  dep_found = False
  for dependency in api.dependencies:
    if dep_name == dependency['name']:
      dep_found = True
      match_found = True
      version = dependency['version']
      if dep_version_lower is not None:
        comp = compare_version(dep_version_lower, version)
        if dep_ex_lower and comp >= 0:
          match_found = False
        elif not dep_ex_lower and comp > 0:
          match_found = False

      if not match_found:
        continue

      if dep_version_higher is not None:
        comp = compare_version(dep_version_higher, version)
        if dep_ex_higher and comp <= 0:
          match_found = False
        elif not dep_ex_higher and comp < 0:
          match_found = False

      if match_found:
        return
  if dep_found:
    range_str = get_version_range_string(dep_version_lower, dep_version_higher, dep_ex_lower, dep_ex_higher)
    raise EagerPolicyAssertionException('Version of required dependency {0} is not in ' \
                                        'the range {1}'.format(dep_name, range_str))
  else:
    raise EagerPolicyAssertionException('Required dependency {0} not used'.format(dep_name))


def is_api_equal(name1, version1, name2, version2):
  if version2 is not None:
    return name1 == name2 and version1 == version2
  else:
    return name1 == name2

def compare_version(version1, version2):
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

