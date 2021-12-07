--Metadata for the dialect is migrated to the .sqlserver namespace
UPDATE core.metadata
SET namespace = 'uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver'
WHERE namespace = 'uk.ac.ox.softeng.maurodatamapper.plugins.database'
AND multi_facet_aware_item_domain_type = 'DataModel'
AND key = 'dialect'
AND value = 'MS SQL Server';

--Metadata on DataElement is migrated to the .sqlserver.column namespace
UPDATE core.metadata
SET namespace = 'uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.column'
WHERE namespace = 'uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver'
AND multi_facet_aware_item_domain_type = 'DataElement';

--Metadata on DataClass where the DataClass is a table rather than a schema is migrated to the .sqlserver.table namespace
UPDATE core.metadata
SET namespace = 'uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.table'
WHERE namespace = 'uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver'
AND multi_facet_aware_item_domain_type = 'DataClass'
AND multi_facet_aware_item_id IN (
    SELECT id FROM datamodel.data_class
	WHERE parent_data_class_id IS NOT NULL
);

--No migration to .sqlserver.schema because there isn't a profile for schema

