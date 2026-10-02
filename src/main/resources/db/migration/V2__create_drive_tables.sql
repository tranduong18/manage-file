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

CREATE TABLE files (
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
   CONSTRAINT uq_file_user_gid UNIQUE (user_id, google_file_id)
);
CREATE INDEX idx_file_user_parent ON files (user_id, parent_folder_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_file_user_name   ON files (user_id, lower(file_name));
CREATE INDEX idx_file_user_mime   ON files (user_id, mime_type);

CREATE TABLE activity_log (
  id             VARCHAR(36)  PRIMARY KEY,
  user_id        VARCHAR(36)  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  file_id        VARCHAR(36)  REFERENCES files(id) ON DELETE SET NULL,
  action         VARCHAR(16)  NOT NULL,
  status         VARCHAR(16)  NOT NULL,
  error_message  TEXT,
  created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_activity_user_time ON activity_log (user_id, created_at DESC);