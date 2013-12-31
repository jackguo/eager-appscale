class TypeCheckerException(Exception):
  pass

class DataType(object):
  pass

class PrimitiveType(DataType):
  PRIMITIVES = ( 'short', 'int', 'long', 'string', 'boolean', 'double', 'byte', 'binary' )

  def __init__(self, name):
    if name in self.PRIMITIVES:
      self.name = name
    else:
      raise TypeCheckerException('Invalid primitive type: ' + str(name))

class ContainerType(DataType):
  CONTAINERS = ( 'list', 'set' )

  def __init__(self, container, child):
    if container in self.CONTAINERS:
      self.container = container
      self.child = child
    else:
      raise TypeCheckerException('Invalid container type: ' + str(container))

class Field:
  def __init__(self, name, data_type, optional = True):
    self.name = name
    self.data_type = data_type
    self.optional = optional

class ComplexType(DataType):
  def __init__(self, name, fields):
    if isinstance(fields, list):
      self.name = name
      self.fields = list(fields)
    else:
      raise TypeCheckerException('Fields must be a list')

def is_input_compatible(type1, type2):
  if isinstance(type1, PrimitiveType):
    compatible = isinstance(type2, PrimitiveType) and type1.name == type2.name
    if not compatible:
      print 'Primitive match incompatible'
    return compatible
  elif isinstance(type1, ContainerType):
    compatible = isinstance(type2, ContainerType) and type1.container == type2.container and is_input_compatible(type1.child, type2.child)
    if not compatible:
      print 'Container match incompatible'
    return compatible
  elif isinstance(type1, ComplexType):
    if not isinstance(type2, ComplexType): return False
    for f1 in type1.fields:
      match_found = None
      for f2 in type2.fields:
        if f1.name == f2.name:
          compatible = is_input_compatible(f1.data_type, f2.data_type)
          if not compatible:
            print 'Field ' + f1.name + ' type incompatible'
            return False
          match_found = f2
          break
      if match_found is not None: type2.fields.remove(match_found)
    for f2 in type2.fields:
      if not f2.optional:
        print 'Extra field ' + f2.name + ' not optional'
        return False
    return True
  else:
    raise TypeCheckerException('Illegal type arguments')

def is_output_compatible(type1, type2, errors):
  if isinstance(type1, PrimitiveType):
    compatible = isinstance(type2, PrimitiveType) and type1.name == type2.name
    if not compatible:
      errors.append('Primitive type match incompatible')
    return compatible
  elif isinstance(type1, ContainerType):
    compatible = isinstance(type2, ContainerType) and type1.container == type2.container and is_output_compatible(type1.child, type2.child, errors)
    if not compatible:
      errors.append('Container type match incompatible')
    return compatible
  elif isinstance(type1, ComplexType):
    if not isinstance(type2, ComplexType): return False
    for f1 in type1.fields:
      match_found = None
      for f2 in type2.fields:
        if f1.name == f2.name:
          compatible = is_output_compatible(f1.data_type, f2.data_type, errors)
          if not compatible:
            errors.append('Field ' + f1.name + ' type incompatible')
            return False
          match_found = f2
          break
      if match_found is None:
        errors.append('No match found for field ' + f1.name)
        return False
      else:
        type2.fields.remove(match_found)
    return True
  else:
    raise TypeCheckerException('Illegal type arguments')