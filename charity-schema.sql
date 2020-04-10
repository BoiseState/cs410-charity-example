DROP TABLE IF EXISTS gift_fund_allocation CASCADE;
DROP TABLE IF EXISTS gift CASCADE;
DROP TABLE IF EXISTS fund CASCADE;
DROP TABLE IF EXISTS donor CASCADE;

CREATE TABLE donor (
  donor_id SERIAL PRIMARY KEY,
  donor_name VARCHAR(500) NOT NULL,
  donor_email VARCHAR(200) NOT NULL,
  donor_address VARCHAR(200) NOT NULL,
  donor_city VARCHAR(100) NOT NULL,
  donor_state VARCHAR(20) NOT NULL,
  donor_zip VARCHAR(10) NOT NULL
);

CREATE TABLE fund (
  fund_id INTEGER PRIMARY KEY,
  fund_name VARCHAR(50) NOT NULL
);

CREATE TABLE gift (
  gift_id SERIAL PRIMARY KEY,
  donor_id INTEGER NOT NULL,
  gift_date DATE NOT NULL,

  FOREIGN KEY (donor_id) REFERENCES donor (donor_id)
);
CREATE INDEX gift_donor_idx ON gift (donor_id);

CREATE TABLE gift_fund_allocation (
  gf_alloc_id SERIAL PRIMARY KEY,
  gift_id INTEGER NOT NULL REFERENCES gift,
  fund_id INTEGER NOT NULL REFERENCES fund,
  amount DECIMAL NOT NULL,
  
  FOREIGN KEY (gift_id) REFERENCES gift (gift_id),
  FOREIGN KEY (fund_id) REFERENCES fund (fund_id)
);
CREATE INDEX gfa_gift_idx ON gift_fund_allocation (gift_id);
CREATE INDEX gfa_fund_idx ON gift_fund_allocation (fund_id);
