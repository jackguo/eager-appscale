def is_api_compatible(old_spec, new_spec, ops=[]):
  old_api = old_spec['apis'][0]
  new_api = new_spec['apis'][0]

  errors = []

  if old_spec['basePath'] != new_spec['basePath']:
    errors.append("Base paths are incompatible: Old API={0}; New API={1}".format(
      old_spec['basePath'], new_spec['basePath']))

  if old_spec['resourcePath'] != new_spec['resourcePath']:
    errors.append("Resource paths are incompatible: Old API={0}; New API={1}".format(
      old_spec['resourcePath'], new_spec['resourcePath']))

  for old_operation in old_api['operations']:
    if is_related_operation(old_operation, ops):
      new_operation = find_operation(new_api, old_operation)
      if not new_operation:
        errors.append("Operation '{0}' not present in the new API".format(old_operation['nickname']))
      else:
        errors += is_operation_compatible(old_operation, old_spec, new_operation, new_spec)

  if not errors:
    return True, None
  else:
    return False, '|'.join(errors)

def is_operation_compatible(old_op, old_spec, new_op, new_spec):
  errors = []
  op_name = old_op['nickname']
  if old_op['method'] != new_op['method']:
    errors.append("HTTP method incompatibility in operation '{0}': Old API = {1}; " \
                  "New API = {2}".format(op_name, old_op['method'], new_op['method']))
  return errors

def find_operation(api, key_op):
  operations = api['operations']
  for op in operations:
    if op['nickname'] == key_op['nickname']:
      return op

  for op in operations:
    if op['method'] == key_op['method']:
      return op
  return None

def is_related_operation(operation, ops):
  if not ops:
    return True
  else:
    return operation in ops