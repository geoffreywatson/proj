# USER schema

# --- !Ups
create table USER (
  ID BIGINT PRIMARY KEY AUTO_INCREMENT,
  EMAIL VARCHAR(255) NOT NULL,
  PSWD_HASH INT NOT NULL,
  ROLE VARCHAR(10) NOT NULL,
  CREATED TIMESTAMP NOT NULL
  );

create table CONTACT(
  ID BIGINT PRIMARY KEY AUTO_INCREMENT,
  UID BIGINT,
  CREATED TIMESTAMP NOT NULL,
  TITLE VARCHAR(5),
  FIRST_NAME VARCHAR(255),
  MIDDLE_NAME VARCHAR(255),
  LAST_NAME VARCHAR(255),
  DOB DATE,
  NIN VARCHAR(9),
  FOREIGN KEY (uid) REFERENCES USER(id)
  );

create table ADDRESS(
  ID BIGINT PRIMARY KEY AUTO_INCREMENT,
  LINE1 VARCHAR(255),
  LINE2 VARCHAR(255),
  LINE3 VARCHAR(255),
  CITY VARCHAR(50),
  COUNTY VARCHAR(50),
  POSTCODE VARCHAR(20),
  COUNTRY VARCHAR(50),
  CREATED TIMESTAMP
);

# --- !Downs
drop table USER;
drop table CONTACT;
drop table ADDRESS;