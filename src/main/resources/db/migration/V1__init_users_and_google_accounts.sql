CREATE TABLE users (
   id          VARCHAR(36) PRIMARY KEY,
   email       VARCHAR(255) NOT NULL UNIQUE,
   name        VARCHAR(255),
   avatar_url  VARCHAR(1024),
   created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE google_accounts (
     id                  VARCHAR(36) PRIMARY KEY,
     user_id             VARCHAR(36) NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
     access_token_enc    TEXT NOT NULL,
     refresh_token_enc   TEXT,
     expires_at          TIMESTAMPTZ NOT NULL,
     scopes              VARCHAR(512) NOT NULL,
     created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
     updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);