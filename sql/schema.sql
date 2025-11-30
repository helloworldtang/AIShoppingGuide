CREATE TABLE user_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  external_id VARCHAR(64) UNIQUE,
  name VARCHAR(128),
  segment VARCHAR(64),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES user_profile(id)
);

CREATE TABLE request_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  request_type VARCHAR(32),
  status VARCHAR(32),
  started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP NULL,
  latency_ms INT,
  error_code VARCHAR(32),
  FOREIGN KEY (session_id) REFERENCES session(id)
);

CREATE TABLE intent_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_id BIGINT NOT NULL,
  is_guide TINYINT(1),
  is_standard TINYINT(1),
  category VARCHAR(128),
  brand_split_needed TINYINT(1),
  keywords_json JSON,
  confidence DECIMAL(5,2),
  FOREIGN KEY (request_id) REFERENCES request_log(id)
);

CREATE TABLE standard_question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(512),
  normalized_hash VARCHAR(64) UNIQUE,
  answer TEXT,
  category VARCHAR(128),
  enabled TINYINT(1) DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) UNIQUE,
  parent_id BIGINT NULL
);

CREATE TABLE brand (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) UNIQUE
);

CREATE TABLE product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT NOT NULL,
  brand_id BIGINT NOT NULL,
  sku VARCHAR(128) UNIQUE,
  name VARCHAR(256),
  spec_json JSON,
  price DECIMAL(12,2),
  stock INT,
  url VARCHAR(512),
  enabled TINYINT(1) DEFAULT 1,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (category_id) REFERENCES product_category(id),
  FOREIGN KEY (brand_id) REFERENCES brand(id)
);

CREATE TABLE competitor_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  source_brand VARCHAR(128),
  source_sku VARCHAR(128),
  target_brand VARCHAR(128),
  target_sku VARCHAR(128),
  rule_json JSON,
  confidence DECIMAL(5,2),
  enabled TINYINT(1) DEFAULT 1,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE recommendation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_id BIGINT NOT NULL,
  product_id BIGINT,
  rank INT,
  reason TEXT,
  score DECIMAL(6,3),
  link_url VARCHAR(512),
  FOREIGN KEY (request_id) REFERENCES request_log(id),
  FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE tracking_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rec_id BIGINT,
  user_id BIGINT,
  session_id BIGINT,
  event_type VARCHAR(64),
  product_id BIGINT,
  url VARCHAR(512),
  ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  metadata_json JSON
);

CREATE TABLE long_memory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  key_name VARCHAR(256) UNIQUE,
  type VARCHAR(64),
  content TEXT,
  embedding_id VARCHAR(128),
  source VARCHAR(64),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE prompt_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128),
  version VARCHAR(32),
  role VARCHAR(32),
  content TEXT,
  enabled TINYINT(1) DEFAULT 1,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE llm_call_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  request_id BIGINT,
  model VARCHAR(64),
  prompt_template_id BIGINT,
  success TINYINT(1),
  latency_ms INT,
  tokens_input INT,
  tokens_output INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
