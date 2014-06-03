import re

regex = re.compile('[A-Z].*')
assert_true(regex.match(app.name), 'Application names must start with an uppercase letter')

regex = re.compile('admin@test.com')
assert_true(regex.match(app.owner), 'Application username must match admin@test.com')

regex = re.compile('[A-Z].*')
for api in app.api_list:
  assert_true(regex.match(api.name), 'API names must start with an uppercase letter')
