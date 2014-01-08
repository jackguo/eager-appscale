class API(tuple):
  __slots__ = []

  def __new__(cls, name, version, dependencies):
    return tuple.__new__(cls, (name, version, dependencies))

  @property
  def name(self):
    return tuple.__getitem__(self, 0)

  @property
  def version(self):
    return tuple.__getitem__(self, 1)

  @property
  def dependencies(self):
    return tuple.__getitem__(self, 2)

  def __getitem__(self, item):
    raise TypeError
