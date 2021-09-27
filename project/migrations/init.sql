CREATE TABLE users
(
    uuid     UUID PRIMARY KEY,
    name     VARCHAR UNIQUE NOT NULL,
    password VARCHAR        NOT NULL,
    deleted  BOOLEAN        NOT NULL DEFAULT false
);

CREATE TABLE posts
(
    uuid    UUID PRIMARY KEY,
    message VARCHAR NOT NULL,
    user_id UUID    NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT user_id_fkey FOREIGN KEY (user_id)
        REFERENCES users (uuid) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE
EXTENSION ltree;

CREATE TABLE comments
(
    uuid         UUID PRIMARY KEY,
    message      VARCHAR NOT NULL,
    user_id      UUID    NOT NULL,
    CONSTRAINT user_id_fkey FOREIGN KEY (user_id)
        REFERENCES users (uuid) MATCH SIMPLE
        ON UPDATE NO ACTION ON DELETE NO ACTION,
    comment_path ltree   NOT NULL,
    deleted      BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE tags
(
    uuid    UUID PRIMARY KEY,
    name    VARCHAR NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE posts_tags
(
    post_id UUID NOT NULL REFERENCES posts (uuid),
    tag_id  UUID NOT NUll REFERENCES tags (uuid),
    PRIMARY KEY (post_id, tag_id)
);

