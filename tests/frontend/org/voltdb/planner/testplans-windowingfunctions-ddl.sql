DROP TABLE AAA           IF EXISTS;
DROP TABLE BBB           IF EXISTS;
DROP TABLE AAA_PA        IF EXISTS;
DROP TABLE AAA_PB        IF EXISTS;
DROP TABLE AAA_PC        IF EXISTS;
DROP TABLE AAA_STRING    IF EXISTS;
DROP TABLE AAA_STRING_PA IF EXISTS;
DROP TABLE AAA_TIMESTAMP IF EXISTS;

CREATE TABLE AAA (
    A INTEGER,
    B INTEGER,
    C INTEGER,
);

CREATE TABLE BBB (
    A INTEGER,
    B INTEGER,
    C INTEGER,
);

CREATE TABLE AAA_PA (
    A INTEGER NOT NULL,
    B INTEGER NOT NULL,
    C INTEGER NOT NULL,
);
PARTITION TABLE AAA_PA ON COLUMN A;

CREATE TABLE AAA_STRING_PA (
    A VARCHAR NOT NULL,
    B INTEGER NOT NULL,
    C INTEGER NOT NULL,
);
PARTITION TABLE AAA_STRING_PA ON COLUMN A;

CREATE TABLE AAA_TIMESTAMP (
    A TIMESTAMP NOT NULL,
    B INTEGER   NOT NULL,
    C INTEGER   NOT NULL,
);

--
-- Tables for testing the interaction between
-- window functions, statement level order bys
-- and indexes.  There's a plain vanilla table,
-- a table with an index, a partitioned table and
-- a partitioned table with an index.
--
DROP TABLE VANILLA            IF EXISTS;
DROP TABLE VANILLA_IDX        IF EXISTS;
DROP TABLE VANILLA_PA         IF EXISTS;
DROP TABLE VANILLA_PA_IDX     IF EXISTS;
DROP TABLE VANILLA_PB_IDX     IF EXISTS;

-- This is a plain jane table, with no
-- partition columns and no indexes.
CREATE TABLE VANILLA (
	A	INTEGER NOT NULL,
	B	INTEGER NOT NULL,
	C	INTEGER NOT NULL
);

-- This is a replicated table with an
-- index.
CREATE TABLE VANILLA_IDX (
A	INTEGER NOT NULL,
B	INTEGER NOT NULL,
C	INTEGER NOT NULL
);
CREATE INDEX IVANILLA_IDXA ON VANILLA_IDX (A);
CREATE INDEX IVANILLA_IDXAB ON VANILLA_IDX(A, B);

-- This is a partitioned table with no indexes.
CREATE TABLE VANILLA_PA (
	A	INTEGER NOT NULL,
	B	INTEGER NOT NULL,
	C	INTEGER NOT NULL
);

PARTITION TABLE VANILLA_PA ON COLUMN A;

-- This is a partitioned table with an index
-- on the partition column.
CREATE TABLE VANILLA_PA_IDX (
A	INTEGER NOT NULL,
B	INTEGER NOT NULL,
C	INTEGER NOT NULL
);
PARTITION TABLE VANILLA_PA_IDX ON COLUMN A;
CREATE INDEX IVANILLA_PA_IDX ON VANILLA_PA_IDX (A);

-- This is a partitioned table with no index on the
-- partition column, but with an index on some
-- other column.
CREATE TABLE VANILLA_PB_IDX (
A	INTEGER NOT NULL,
B	INTEGER NOT NULL,
C	INTEGER NOT NULL
);
PARTITION TABLE VANILLA_PB_IDX ON COLUMN B;
CREATE INDEX IVANILLA_PB_IDX ON VANILLA_PB_IDX (A);

CREATE TABLE O3 (
 PK1 INTEGER NOT NULL,
 PK2 INTEGER NOT NULL,
 I3  INTEGER NOT NULL,
 I4  INTEGER NOT NULL,
 PRIMARY KEY (PK1, PK2)
 );

CREATE INDEX O3_TREE ON O3 (I3);
CREATE INDEX O3_PARTIAL_TREE ON O3 (I4) WHERE PK2 > 0;
CREATE PROCEDURE Truncate03 AS DELETE FROM O3;

CREATE TABLE O4 (
 ID    INTEGER,
 CTR   INTEGER
 );
CREATE INDEX O4_CTR_PLUS_100 ON O4 (CTR + 100);

--
-- This is from sqlcoverage.
--
CREATE TABLE P_DECIMAL (
ID INTEGER NOT NULL,
CASH DECIMAL NOT NULL,
CREDIT DECIMAL,
RATIO FLOAT,
PRIMARY KEY (ID)
);
PARTITION TABLE P_DECIMAL ON COLUMN ID;
