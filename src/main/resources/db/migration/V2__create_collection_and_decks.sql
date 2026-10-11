CREATE TABLE owned_packs (
    user_id   UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    pack_code VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, pack_code)
);

CREATE TABLE decks (
    id         UUID PRIMARY KEY,
    owner_id   UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    hero_code  VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_decks_owner_updated ON decks (owner_id, updated_at);

CREATE TABLE deck_aspects (
    deck_id  UUID        NOT NULL REFERENCES decks (id) ON DELETE CASCADE,
    position INTEGER     NOT NULL,
    aspect   VARCHAR(20) NOT NULL
        CHECK (aspect IN ('aggression', 'justice', 'leadership', 'protection', 'pool')),
    PRIMARY KEY (deck_id, position)
);

CREATE TABLE deck_slots (
    deck_id   UUID        NOT NULL REFERENCES decks (id) ON DELETE CASCADE,
    card_code VARCHAR(20) NOT NULL,
    quantity  INTEGER     NOT NULL CHECK (quantity BETWEEN 1 AND 9),
    PRIMARY KEY (deck_id, card_code)
);
