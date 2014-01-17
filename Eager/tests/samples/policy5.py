for dep in api.dependencies:
  assert_true(dep['version'] == '1.0', 'All dependencies should be v1.0')