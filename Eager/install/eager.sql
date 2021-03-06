CREATE TABLE EAGER_API_DEPENDENCY (
  EAGER_DEPENDENCY_NAME VARCHAR(200) NOT NULL,
  EAGER_DEPENDENCY_VERSION VARCHAR(30) NOT NULL,
  EAGER_DEPENDENT_NAME VARCHAR(200) NOT NULL,
  EAGER_DEPENDENT_VERSION VARCHAR(30) NOT NULL,
  EAGER_DEPENDENCY_OPERATIONS VARCHAR(200)
)ENGINE INNODB;

CREATE TABLE EAGER_API_SPEC (
  EAGER_API_NAME VARCHAR(200) NOT NULL,
  EAGER_API_VERSION VARCHAR(30) NOT NULL,
  EAGER_API_SPEC_TEXT TEXT,
  PRIMARY KEY (EAGER_API_NAME, EAGER_API_VERSION)
)ENGINE INNODB;

CREATE INDEX EDGE_BY_DEPENDENCY ON EAGER_API_DEPENDENCY (EAGER_DEPENDENCY_NAME, EAGER_DEPENDENCY_VERSION);
CREATE INDEX EDGE_BY_DEPENDENT ON EAGER_API_DEPENDENCY (EAGER_DEPENDENT_NAME, EAGER_DEPENDENT_VERSION);