import re

for dep in app.dependencies:
  assert_true(dep['version'] == '1.0', 'All dependencies should be v1.0')

regex = re.compile('[A-Z].*')
for api in app.api_list:
  assert_true(regex.match(api.name), 'API names must start with an uppercase letter')