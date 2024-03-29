CREATE DATABASE metadata_simple;
GO
USE metadata_simple;
GO

EXECUTE sys.sp_addextendedproperty
@name = N'info',
@value = N'A database called metadata_simple which is used for integration testing';

EXECUTE sys.sp_addextendedproperty
@name = N'SCHEMA-DESCRIPTION',
@value = N'Contains objects used for testing',
@level0type = N'SCHEMA',
@level0name = 'dbo';


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
  org_char CHAR(5),
  org_nvarchar NVARCHAR(20),
  org_nchar NCHAR(6)
);
--Use both VARCHAR and CHAR columns. Expect that because there are no CHAR columns other than org_char,
--when org_char is detected as an enumeration, CHAR will be removed from the primitive data types
INSERT INTO organisation(id, org_name, org_type, org_code, description, org_char, org_nvarchar, org_nchar) VALUES
(1, 'ORG1', 'TYPEA', 'CODEY', 'Description of ORG1', 'CHAR1', 'GREEN', 'ONE'),
(2, 'ORG2', 'TYPEA', 'CODEY', 'Description of ORG2', 'CHAR1', 'GREEN', 'ONE'),
(3, 'ORG3', 'TYPEA', 'CODEY', 'Description of ORG3', 'CHAR1', 'GREEN', 'ONE'),
(4, 'ORG4', 'TYPEA', 'CODEY', 'Description of ORG4', 'CHAR1', 'GREEN', 'ONE'),
(5, 'ORG5', 'TYPEB', 'CODEY', 'Description of ORG5', 'CHAR1', 'GREEN', 'ONE'),
(6, 'ORG6', 'TYPEB', 'CODEY', 'Description of ORG6', 'CHAR1', 'GREEN', 'ONE'),
(7, 'ORG7', 'TYPEB', 'CODEY', 'Description of ORG7', 'CHAR1', 'RED', 'ONE'),
(8, 'ORG8', 'TYPEB', 'CODEY', 'Description of ORG8', 'CHAR2', 'RED', 'ONE'),
(9, 'ORG9', 'TYPEB', 'CODEY', 'Description of ORG9', 'CHAR2', 'RED', 'TWO'),
(10, 'ORG10', 'TYPEA', 'CODEZ', 'Description of ORG10', 'CHAR2', NULL, NULL),
(11, 'ORG11', 'TYPEA', 'CODEZ', 'Description of ORG11', 'CHAR2', '', ''),
(12, 'ORG12', 'TYPEA', 'CODEZ', 'Description of ORG12', 'CHAR2', '', ''),
(13, 'ORG13', 'TYPEA', 'CODEZ', 'Description of ORG13', 'CHAR2', 'RED', 'TWO'),
(14, 'ORG14', 'TYPEA', 'CODEZ', 'Description of ORG14', 'CHAR2', 'RED', 'TWO'),
(15, 'ORG15', 'TYPEB', 'CODEZ', 'Description of ORG15', 'CHAR2', 'RED', 'TWO'),
(16, 'ORG16', 'TYPEB', 'CODEZ', 'Description of ORG16', 'CHAR2', 'RED', 'TWO'),
(17, 'ORG17', 'TYPEB', 'CODEZ', 'Description of ORG17', 'CHAR2', 'RED', 'TWO'),
(18, 'ORG18', 'TYPEB', 'CODEZ', 'Description of ORG18', 'CHAR2', 'PURPLE', 'TWO'),
(19, 'ORG19', 'TYPEB', 'CODEZ', 'Description of ORG19', 'CHAR2', 'PURPLE', 'TWO'),
(20, 'ORG20', 'TYPEB', 'CODEZ', 'Description of ORG20', 'CHAR2', 'PURPLE', 'TWO'),
(21, 'ORG21', 'TYPEA', 'CODEX', 'Description of ORG21', 'CHAR3', 'PURPLE', 'TWO'),
(22, 'ORG22', 'TYPEA', 'CODEX', 'Description of ORG22', 'CHAR3', 'PURPLE', 'TWO'),
(23, 'ORG23', 'TYPEA', 'CODEX', 'Description of ORG23', 'CHAR3', 'PURPLE', 'TWO'),
(24, 'ORG24', 'TYPEA', 'CODEX', 'Description of ORG24', 'CHAR3', 'PURPLE', 'TWO'),
(25, 'ORG25', 'TYPEB', 'CODEX', 'Description of ORG25', 'CHAR3', 'PURPLE', 'TWO'),
(26, 'ORG26', 'TYPEB', 'CODEX', 'Description of ORG26', 'CHAR3', 'PURPLE', 'THREE'),
(27, 'ORG27', 'TYPEB', 'CODEX', 'Description of ORG27', 'CHAR3', 'PURPLE', 'THREE'),
(28, 'ORG28', 'TYPEB', 'CODEX', 'Description of ORG28', 'CHAR3', 'PURPLE', 'THREE'),
(29, 'ORG29', 'TYPEB', 'CODEX', 'Description of ORG29', 'CHAR3', 'PURPLE', 'THREE'),
(30, 'ORG30', 'TYPEB', 'CODEX', 'Description of ORG30', 'CHAR3', 'PURPLE', 'THREE'),
(31, 'ORG31', 'TYPEA', 'CODEX', 'Description of ORG31', 'CHAR3', 'PURPLE', 'FOUR'),
(32, 'ORG32', 'TYPEA', 'CODEX', 'Description of ORG32', 'CHAR3', 'PURPLE', 'FOUR'),
(33, 'ORG33', 'TYPEA', 'CODEX', 'Description of ORG33', 'CHAR3', 'PURPLE', 'FOUR'),
(34, 'ORG34', 'TYPEA', 'CODEX', 'Description of ORG34', 'CHAR3', 'PURPLE', 'FOUR'),
(35, 'ORG35', 'TYPEC', 'CODEX', 'Description of ORG35', 'CHAR3', 'PURPLE', 'FIVE'),
(36, 'ORG36', 'TYPEC', 'CODEX', 'Description of ORG36', 'CHAR3', 'PURPLE', 'FIVE'),
(37, 'ORG37', 'TYPEB', 'CODEX', 'Description of ORG37', 'CHAR3', 'PURPLE', 'FIVE'),
(38, 'ORG38', 'TYPEB', 'CODEX', 'Description of ORG38', 'CHAR3', 'PURPLE', 'SIX'),
(39, 'ORG39', 'TYPEB', 'CODEX', 'Description of ORG39', 'CHAR3', 'PURPLE', 'SIX'),
(40, 'ORG40', 'TYPEB', 'CODER', 'Description of ORG40', '', 'PURPLE', 'SIX'),
(41, 'ORG41', 'TYPEB', 'CODER', 'Description of ORG41', null, 'PURPLE', 'SEVEN');

EXEC sp_addextendedproperty
@name = N'DESCRIPTION', @value = 'A table about organisations',
@level0type = N'Schema', @level0name = 'dbo',
@level1type = N'Table', @level1name = 'organisation'
GO

EXEC sp_addextendedproperty
@name = N'PROPERTY1', @value = 'A first extended property on org_code',
@level0type = N'Schema', @level0name = 'dbo',
@level1type = N'Table', @level1name = 'organisation',
@level2type = N'Column',@level2name = 'org_code';
GO

EXEC sp_addextendedproperty
@name = N'PROPERTY2', @value = 'A second extended property on org_code',
@level0type = N'Schema', @level0name = 'dbo',
@level1type = N'Table', @level1name = 'organisation',
@level2type = N'Column',@level2name = 'org_code';
GO

CREATE TABLE sample
(
  id INT NOT NULL PRIMARY KEY IDENTITY,
  sample_tinyint TINYINT,
  sample_smallint SMALLINT,
  sample_int INT,
  sample_bigint BIGINT,
  sample_decimal DECIMAL(12,3),
  sample_numeric NUMERIC(10,6),
  sample_date DATE,
  sample_smalldatetime SMALLDATETIME,
  sample_datetime DATETIME,
  sample_datetime2 DATETIME2
);

--sample data: sample_smallint goes from -100 to 100. sample_int goes from 0 to 10000.
--sample_bigint goes from -1000000 to 1000000
WITH populate AS (
SELECT -100 AS x UNION ALL SELECT x + 1 FROM populate WHERE x < 100
)
INSERT INTO sample (sample_tinyint, sample_smallint, sample_int, sample_bigint, sample_decimal, sample_numeric, sample_date, sample_smalldatetime, sample_datetime, sample_datetime2)
SELECT ABS(x), x, x*x, x*x*x, x*x * 573, x*x*x / 104756.576, DATEADD(day, x, '2020-09-01'), DATEADD(month, x, '2020-09-01'), DATEADD(year, x, '2020-09-01'), DATEADD(hour, x, '2020-09-01') FROM populate
OPTION (MAXRECURSION 0);

CREATE TABLE bigger_sample (
sample_bigint BIGINT,
sample_decimal DECIMAL(12, 3),
sample_date DATE,
sample_varchar VARCHAR(20)
);
WITH populate AS (
SELECT 1 AS x UNION ALL SELECT x + 1 FROM populate WHERE x < 500000
)
INSERT INTO bigger_sample (sample_bigint) SELECT x FROM populate
OPTION (MAXRECURSION 0);
UPDATE bigger_sample
SET sample_decimal = SIN(sample_bigint),
sample_date = DATEADD(day, 200 * SIN(sample_bigint), '2020-09-02'),
sample_varchar = 'ENUM' + CONVERT(VARCHAR(2), sample_bigint % 15);
GO
CREATE VIEW bigger_sample_view AS SELECT * FROM bigger_sample;
GO