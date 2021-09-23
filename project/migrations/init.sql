CREATE TABLE users (
    uuid UUID PRIMARY KEY,
    name VARCHAR UNIQUE NOT NULL,
    password VARCHAR NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE posts (
    uuid UUID PRIMARY KEY,
    message VARCHAR NOT NULL,
    user_id UUID NOT NULL,
    CONSTRAINT user_id_fkey FOREIGN KEY (user_id)
        REFERENCES users (uuid) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE EXTENSION ltree;

CREATE TABLE comments (
    uuid UUID PRIMARY KEY,
    message VARCHAR NOT NULL,
    user_id UUID NOT NULL,
    CONSTRAINT user_id_fkey FOREIGN KEY (user_id)
        REFERENCES users (uuid) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION,
    comment_path ltree NOT NULL
);