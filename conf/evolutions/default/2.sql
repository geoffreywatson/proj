# --- !Ups

ALTER TABLE loan_application MODIFY OFFER_APR DECIMAL(10,4);

# --- !Downs

ALTER TABLE loan_application MODIFY OFFER_APR NUMERIC;
