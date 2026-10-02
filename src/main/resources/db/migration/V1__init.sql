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

CREATE TABLE folders (
     id                VARCHAR(36)  PRIMARY KEY,
     user_id           VARCHAR(36)  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
     google_folder_id  VARCHAR(128) NOT NULL,
     name              VARCHAR(512) NOT NULL,
     parent_google_id  VARCHAR(128),
     source            VARCHAR(16)  NOT NULL DEFAULT 'CREATED',
     created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
     CONSTRAINT uq_folders_user_gid UNIQUE (user_id, google_folder_id)
);
CREATE INDEX idx_folders_user_parent ON folders (user_id, parent_google_id);

CREATE TABLE file_metadata (
       id                VARCHAR(36)  PRIMARY KEY,
       user_id           VARCHAR(36)  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
       google_file_id    VARCHAR(128) NOT NULL,
       file_name         VARCHAR(512) NOT NULL,
       mime_type         VARCHAR(255),
       size_bytes        BIGINT,
       parent_folder_id  VARCHAR(128),
       thumbnail_link    TEXT,
       web_view_link     TEXT,
       source            VARCHAR(16)  NOT NULL DEFAULT 'UPLOADED',
       is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
       uploaded_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
       updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
       CONSTRAINT uq_file_metadata_user_gid UNIQUE (user_id, google_file_id)
);
CREATE INDEX idx_file_metadata_user_parent ON file_metadata (user_id, parent_folder_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_file_metadata_user_name   ON file_metadata (user_id, lower(file_name));
CREATE INDEX idx_file_metadata_user_mime   ON file_metadata (user_id, mime_type);