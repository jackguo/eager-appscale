"""Dynamically decide from where to import Google App Engine modules.

All other NDB code should import its Google App Engine modules from
this module.  If necessary, add new imports here (in both places).
"""

try:
  from google.appengine import api
  normal_environment = True
except ImportError:
  from google3.apphosting import api
  normal_environment = False

if normal_environment:
  from google.appengine.api.blobstore import blobstore as api_blobstore
  from google.appengine.api import apiproxy_rpc
  from google.appengine.api import apiproxy_stub_map
  from google.appengine.api import datastore
  from google.appengine.api import datastore_errors
  from google.appengine.api import datastore_types
  from google.appengine.api import memcache
  from google.appengine.api import namespace_manager
  from google.appengine.api import prospective_search
  from google.appengine.api import taskqueue
  from google.appengine.api import urlfetch
  from google.appengine.api import users
  from google.appengine.api.prospective_search import prospective_search_pb
  from google.appengine.datastore import datastore_query
  from google.appengine.datastore import datastore_rpc
  from google.appengine.datastore import entity_pb
  from google.appengine.ext.blobstore import blobstore as ext_blobstore
  from google.appengine.ext import db
  from google.appengine.ext import gql
  from google.appengine.runtime import apiproxy_errors
  from google.net.proto import ProtocolBuffer
else:
  from google3.apphosting.api.blobstore import blobstore as api_blobstore
  from google3.apphosting.api import apiproxy_rpc
  from google3.apphosting.api import apiproxy_stub_map
  from google3.apphosting.api import datastore
  from google3.apphosting.api import datastore_errors
  from google3.apphosting.api import datastore_types
  from google3.apphosting.api import memcache
  from google3.apphosting.api import namespace_manager
  from google3.apphosting.api import taskqueue
  from google3.apphosting.api import urlfetch
  from google3.apphosting.api import users
  from google3.apphosting.datastore import datastore_query
  from google3.apphosting.datastore import datastore_rpc
  from google3.storage.onestore.v3 import entity_pb
  from google3.apphosting.ext.blobstore import blobstore as ext_blobstore
  from google3.apphosting.ext import db
  from google3.apphosting.ext import gql
  from google3.apphosting.runtime import apiproxy_errors
  from google3.net.proto import ProtocolBuffer
  # Prospective search is optional.
  try:
    from google3.apphosting.api import prospective_search
    from google3.apphosting.api.prospective_search import prospective_search_pb
  except ImportError:
    pass
