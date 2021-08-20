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
CREATE TABLE organisation
(
  id INT NOT NULL PRIMARY KEY,
  org_name VARCHAR(255) NOT NULL,
  org_type VARCHAR(20) NOT NULL,
  org_code VARCHAR(15) NOT NULL,
  description VARCHAR(MAX),
  org_char CHAR(5)
);
--Use both VARCHAR and CHAR columns. Expect that because there are no CHAR columns other than org_char,
--when org_char is detected as an enumeration, CHAR will be removed from the primitive data types
INSERT INTO organisation(id, org_name, org_type, org_code, description, org_char) VALUES
(1, 'ORG1', 'TYPEA', 'CODEY', 'Description of ORG1', 'CHAR1'),
(2, 'ORG2', 'TYPEA', 'CODEY', 'Description of ORG2', 'CHAR1'),
(3, 'ORG3', 'TYPEA', 'CODEY', 'Description of ORG3', 'CHAR1'),
(4, 'ORG4', 'TYPEA', 'CODEY', 'Description of ORG4', 'CHAR1'),
(5, 'ORG5', 'TYPEB', 'CODEY', 'Description of ORG5', 'CHAR1'),
(6, 'ORG6', 'TYPEB', 'CODEY', 'Description of ORG6', 'CHAR1'),
(7, 'ORG7', 'TYPEB', 'CODEY', 'Description of ORG7', 'CHAR1'),
(8, 'ORG8', 'TYPEB', 'CODEY', 'Description of ORG8', 'CHAR2'),
(9, 'ORG9', 'TYPEB', 'CODEY', 'Description of ORG9', 'CHAR2'),
(10, 'ORG10', 'TYPEA', 'CODEZ', 'Description of ORG10', 'CHAR2'),
(11, 'ORG11', 'TYPEA', 'CODEZ', 'Description of ORG11', 'CHAR2'),
(12, 'ORG12', 'TYPEA', 'CODEZ', 'Description of ORG12', 'CHAR2'),
(13, 'ORG13', 'TYPEA', 'CODEZ', 'Description of ORG13', 'CHAR2'),
(14, 'ORG14', 'TYPEA', 'CODEZ', 'Description of ORG14', 'CHAR2'),
(15, 'ORG15', 'TYPEB', 'CODEZ', 'Description of ORG15', 'CHAR2'),
(16, 'ORG16', 'TYPEB', 'CODEZ', 'Description of ORG16', 'CHAR2'),
(17, 'ORG17', 'TYPEB', 'CODEZ', 'Description of ORG17', 'CHAR2'),
(18, 'ORG18', 'TYPEB', 'CODEZ', 'Description of ORG18', 'CHAR2'),
(19, 'ORG19', 'TYPEB', 'CODEZ', 'Description of ORG19', 'CHAR2'),
(20, 'ORG20', 'TYPEB', 'CODEZ', 'Description of ORG20', 'CHAR2'),
(21, 'ORG21', 'TYPEA', 'CODEX', 'Description of ORG21', 'CHAR3'),
(22, 'ORG22', 'TYPEA', 'CODEX', 'Description of ORG22', 'CHAR3'),
(23, 'ORG23', 'TYPEA', 'CODEX', 'Description of ORG23', 'CHAR3'),
(24, 'ORG24', 'TYPEA', 'CODEX', 'Description of ORG24', 'CHAR3'),
(25, 'ORG25', 'TYPEB', 'CODEX', 'Description of ORG25', 'CHAR3'),
(26, 'ORG26', 'TYPEB', 'CODEX', 'Description of ORG26', 'CHAR3'),
(27, 'ORG27', 'TYPEB', 'CODEX', 'Description of ORG27', 'CHAR3'),
(28, 'ORG28', 'TYPEB', 'CODEX', 'Description of ORG28', 'CHAR3'),
(29, 'ORG29', 'TYPEB', 'CODEX', 'Description of ORG29', 'CHAR3'),
(30, 'ORG30', 'TYPEB', 'CODEX', 'Description of ORG30', 'CHAR3'),
(31, 'ORG31', 'TYPEA', 'CODEX', 'Description of ORG31', 'CHAR3'),
(32, 'ORG32', 'TYPEA', 'CODEX', 'Description of ORG32', 'CHAR3'),
(33, 'ORG33', 'TYPEA', 'CODEX', 'Description of ORG33', 'CHAR3'),
(34, 'ORG34', 'TYPEA', 'CODEX', 'Description of ORG34', 'CHAR3'),
(35, 'ORG35', 'TYPEC', 'CODEX', 'Description of ORG35', 'CHAR3'),
(36, 'ORG36', 'TYPEC', 'CODEX', 'Description of ORG36', 'CHAR3'),
(37, 'ORG37', 'TYPEB', 'CODEX', 'Description of ORG37', 'CHAR3'),
(38, 'ORG38', 'TYPEB', 'CODEX', 'Description of ORG38', 'CHAR3'),
(39, 'ORG39', 'TYPEB', 'CODEX', 'Description of ORG39', 'CHAR3'),
(40, 'ORG40', 'TYPEB', 'CODER', 'Description of ORG40', 'CHAR3');

CREATE TABLE sample
(
  id INT NOT NULL PRIMARY KEY IDENTITY,
  sample_smallint SMALLINT,
  sample_int INT,
  sample_bigint BIGINT
);

--sample data: sample_smallint goes from -100 to 100. sample_int goes from 0 to 10000.
--sample_bigint goes from -1000000 to 1000000
WITH populate AS (
SELECT -100 AS x UNION ALL SELECT x + 1 FROM populate WHERE x < 100
)
INSERT INTO sample (sample_smallint, sample_int, sample_bigint)
SELECT x, x*x, x*x*x FROM populate
OPTION (MAXRECURSION 0);