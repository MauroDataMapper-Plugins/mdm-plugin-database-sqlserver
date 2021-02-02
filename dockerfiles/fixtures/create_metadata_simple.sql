CREATE DATABASE metadata_simple;
GO
USE metadata_simple;
GO
CREATE TABLE catalogue_item
(
  id            UNIQUEIDENTIFIER NOT NULL
    CONSTRAINT catalogue_item_pkey
    PRIMARY KEY,
  version       BIGINT           NOT NULL,
  date_created  DATETIME         NOT NULL,
  domain_type   VARCHAR(255)     NOT NULL,
  last_updated  DATETIME         NOT NULL,
  path          NVARCHAR(MAX)    NOT NULL,
  depth         INTEGER          NOT NULL,
  created_by_id UNIQUEIDENTIFIER NOT NULL,
  label         NVARCHAR(MAX)    NOT NULL,
  description   NVARCHAR(MAX)
);
GO
CREATE INDEX catalogue_item_domain_type_index
  ON catalogue_item (domain_type);
GO
CREATE INDEX catalogue_item_created_by_idx
  ON catalogue_item (created_by_id);
GO
CREATE TABLE catalogue_user
(
  id               UNIQUEIDENTIFIER NOT NULL
    CONSTRAINT catalogue_user_pkey
    PRIMARY KEY,
  version          BIGINT           NOT NULL,
  salt             BINARY           NOT NULL,
  date_created     DATETIME         NOT NULL,
  first_name       VARCHAR(255)     NOT NULL,
  domain_type      VARCHAR(255)     NOT NULL,
  last_updated     DATETIME         NOT NULL,
  organisation     VARCHAR(255),
  user_role        VARCHAR(255)     NOT NULL,
  job_title        VARCHAR(255),
  email_address    VARCHAR(255)     NOT NULL
    CONSTRAINT uk_26qjnuqu76954q376opkqelqd
    UNIQUE,
  user_preferences VARCHAR(255),
  password         BINARY,
  created_by_id    UNIQUEIDENTIFIER
    CONSTRAINT fk3s09b1t9lwqursuetowl2bi9t
    REFERENCES catalogue_user,
  temp_password    VARCHAR(255),
  last_name        VARCHAR(255)     NOT NULL,
  last_login       DATETIME,
  disabled         BIT
);
GO
CREATE INDEX catalogue_user_created_by_idx
  ON catalogue_user (created_by_id);
GO
ALTER TABLE catalogue_item
  ADD CONSTRAINT fkf9kx3d90ixy5pqc1d6kqgjui7
FOREIGN KEY (created_by_id) REFERENCES catalogue_user;
GO
CREATE TABLE metadata
(
  id                UNIQUEIDENTIFIER NOT NULL
    CONSTRAINT metadata_pkey
    PRIMARY KEY,
  version           BIGINT           NOT NULL,
  date_created      DATETIME         NOT NULL,
  domain_type       VARCHAR(255)     NOT NULL,
  catalogue_item_id UNIQUEIDENTIFIER NOT NULL
    CONSTRAINT fkk26px3s00mg783vb5gomhsw07
    REFERENCES catalogue_item,
  last_updated      DATETIME         NOT NULL,
  namespace         NVARCHAR(MAX)    NOT NULL,
  value             NVARCHAR(MAX)    NOT NULL,
  created_by_id     UNIQUEIDENTIFIER NOT NULL
    CONSTRAINT fkfo9b0grugrero8q84mxjst7jr
    REFERENCES catalogue_user,
  md_key            NVARCHAR(MAX)    NOT NULL
);
GO
CREATE UNIQUE INDEX unique_item_id_namespace_key
  ON metadata (catalogue_item_id)
INCLUDE (namespace, md_key);
GO
CREATE INDEX metadata_catalogue_item_idx
  ON metadata (catalogue_item_id);
GO
CREATE INDEX metadata_created_by_idx
  ON metadata (created_by_id);
GO