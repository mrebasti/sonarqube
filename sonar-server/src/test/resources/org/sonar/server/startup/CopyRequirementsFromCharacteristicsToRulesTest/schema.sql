CREATE TABLE "RULES" (
  "ID" INTEGER NOT NULL GENERATED BY DEFAULT AS IDENTITY (START WITH 1, INCREMENT BY 1),
  "PLUGIN_RULE_KEY" VARCHAR(200) NOT NULL,
  "PLUGIN_NAME" VARCHAR(255) NOT NULL,
  "DESCRIPTION" VARCHAR(16777215),
  "NAME" VARCHAR(200),
  "STATUS" VARCHAR(40),
  "CHARACTERISTIC_ID" INTEGER,
  "DEFAULT_CHARACTERISTIC_ID" INTEGER,
  "REMEDIATION_FUNCTION" VARCHAR(20),
  "DEFAULT_REMEDIATION_FUNCTION" VARCHAR(20),
  "REMEDIATION_FACTOR" VARCHAR(20),
  "DEFAULT_REMEDIATION_FACTOR" VARCHAR(20),
  "REMEDIATION_OFFSET" VARCHAR(20),
  "DEFAULT_REMEDIATION_OFFSET" VARCHAR(20),
  "UPDATED_AT" TIMESTAMP
);

CREATE TABLE "CHARACTERISTICS" (
  "ID" INTEGER NOT NULL GENERATED BY DEFAULT AS IDENTITY (START WITH 1, INCREMENT BY 1),
  "PARENT_ID" INTEGER,
  "RULE_ID" INTEGER,
  "FUNCTION_KEY" VARCHAR(100),
  "FACTOR_VALUE" DOUBLE,
  "FACTOR_UNIT" VARCHAR(100),
  "OFFSET_VALUE" DOUBLE,
  "OFFSET_UNIT" VARCHAR(100),
  "ENABLED" BOOLEAN,
  "CREATED_AT" TIMESTAMP,
  "UPDATED_AT" TIMESTAMP
);