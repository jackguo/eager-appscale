import re

regex = re.compile('admin@test.com')
assert_true(regex.match(app.owner))