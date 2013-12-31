from apimgt.typechecker import PrimitiveType, TypeCheckerException, Field, ContainerType, ComplexType, is_output_compatible

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
        errors += compare_operations(old_operation, old_spec, new_operation, new_spec)

  if not errors:
    return True, None
  else:
    return False, '|'.join(errors)

def compare_operations(old_op, old_spec, new_op, new_spec):
  errors = []
  op_name = old_op['nickname']
  if old_op['method'] != new_op['method']:
    errors.append("HTTP method incompatibility in operation '{0}': Old API = {1}; " \
                  "New API = {2}".format(op_name, old_op['method'], new_op['method']))
  errors += compare_output_types(old_op, old_spec, new_op, new_spec)
  return errors

def compare_output_types(old_op, old_spec, new_op, new_spec):
  errors = []
  old_output = get_output_type(old_op)
  new_output = get_output_type(new_op)
  if old_output is not None and new_output is None:
    errors.append("New operation '{0}' has no return type".format(new_op['nickname']))
  elif old_output is None and new_output is not None:
    errors.append("New operation '{0}' has an output type while the corresponding old " \
             "operation doesn't".format(new_op['nickname']))
  elif old_output is not None and new_output is not None:
    old_output_type = swagger_to_eager_type(old_output, old_spec)
    new_output_type = swagger_to_eager_type(new_output, new_spec)
    compatible = is_output_compatible(old_output_type, new_output_type, errors)
    if not compatible:
      errors.append("Output types incompatible: Old API = {0}; New API = {1}".format(
        old_op['nickname'], new_op['nickname']))

  return errors

def get_output_type(operation):
  if operation.get('type'):
    return operation['type']
  elif operation.get('responseMessages'):
    for rm in operation['responseMessages']:
      if 200 <= rm['code'] < 210 and rm.get('responseModel'):
        return rm['responseModel']
  return None

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

def swagger_to_eager_type(data_type, spec, context=None):
  if data_type in PrimitiveType.PRIMITIVES:
    return PrimitiveType(data_type)

  if data_type == 'array':
    if not context:
      raise TypeCheckerException('Unrecognized container type without context')
    child_type = swagger_to_eager_type(context['items']['$ref'], spec)
    unique_items = context.get('uniqueItems') == True
    if unique_items:
      return ContainerType('set', child_type)
    else:
      return ContainerType('list', child_type)

  if spec.get('models'):
    for m_key,model in spec['models'].items():
      if model['id'] == data_type:
        fields = []
        for f_key, value in model['properties'].items():
          child_type_name = value['type']
          f = Field(f_key, swagger_to_eager_type(child_type_name, spec, value))
          if model.get('required') and f_key in model['required']:
            f.optional = False
          fields.append(f)
        return ComplexType(data_type, fields)

  raise TypeCheckerException('Unrecognized data type: {0}'.format(data_type))

