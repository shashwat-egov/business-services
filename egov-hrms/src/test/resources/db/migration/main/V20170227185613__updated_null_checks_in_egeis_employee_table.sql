ALTER TABLE egeis_employee ALTER COLUMN code SET NOT NULL;
ALTER TABLE egeis_employee ALTER COLUMN employeeStatus SET NOT NULL;
ALTER TABLE egeis_employee ALTER COLUMN employeeTypeId SET NOT NULL;
ALTER TABLE egeis_employee ALTER COLUMN physicallyDisabled DROP NOT NULL;
ALTER TABLE egeis_employee ALTER COLUMN medicalReportProduced DROP NOT NULL;
ALTER TABLE egeis_employee ALTER COLUMN passportNo DROP NOT NULL;
ALTER TABLE egeis_employee ALTER COLUMN gpfNo DROP NOT NULL;
ALTER TABLE egeis_employee ALTER COLUMN bankAccount DROP NOT NULL;
ALTER TABLE egeis_employee ALTER COLUMN placeOfBirth DROP NOT NULL;