# Spec: Collection and decks

## Goal

Store each user's card collection (the packs they own) and their decks, so the
frontend can stop using `localStorage` and the data follows the user across
devices. Every query is scoped by the authenticated user.

Out of scope: deck-building rules (deck size, copy limits, aspects allowed for a
hero, signature cards). They depend on card data from MarvelCDB and stay in the
frontend; this API only checks that the data is well formed. Also out of scope:
sharing or publishing decks, deck history, optimistic concurrency between tabs.

## Endpoints

All require a valid JWT. The owner always comes from the token (`sub`).

| Method   | Path              | Body             | Success                              |
|----------|-------------------|------------------|--------------------------------------|
| `GET`    | `/api/collection` |                  | 200, `CollectionResponse`            |
| `PUT`    | `/api/collection` | `CollectionRequest` | 200, `CollectionResponse` (replaces all) |
| `GET`    | `/api/decks`      |                  | 200, `DeckResponse[]`, newest change first |
| `POST`   | `/api/decks`      | `DeckRequest`    | 201, `DeckResponse`, `Location` header |
| `GET`    | `/api/decks/{id}` |                  | 200, `DeckResponse`                  |
| `PUT`    | `/api/decks/{id}` | `DeckRequest`    | 200, `DeckResponse` (replaces fields) |
| `DELETE` | `/api/decks/{id}` |                  | 204                                  |

- `CollectionRequest` / `CollectionResponse`: `packs` (array of pack codes, sorted in responses).
- `DeckRequest`: `name`, `heroCode`, `aspects` (array), `slots` (object: card code to copies).
- `DeckResponse`: `id`, `name`, `heroCode`, `aspects`, `slots`, `createdAt`, `updatedAt`.

## Rules

### Collection (domain: `OwnedPacks`)

- Pack codes are MarvelCDB pack codes: 1 to 50 characters of `a-z`, `0-9` and `_`
  (e.g. `core`, `mut_gen`). Duplicates collapse. At most 500 packs.
- A user with no saved collection has an empty one.

### Decks (domain: `Deck`)

- `name`: trimmed, 1 to 100 characters.
- `heroCode`: a MarvelCDB card code, 1 to 20 characters of `a-z` and `0-9` (e.g. `01001a`).
- `aspects`: 1 to 4 distinct values among `aggression`, `justice`, `leadership`,
  `protection`, `pool`. Order is kept (Spider-Woman's two aspects).
- `slots`: card code (same format as `heroCode`) to copies, 1 to 9 copies each,
  at most 200 different cards and 200 cards in total. A card with 0 copies is
  simply left out of the request.
- Invalid decks *by game rules* (e.g. 30 cards) are accepted: users save while building.
- `createdAt` is set on create; `updatedAt` on create and every update.
- A deck that does not exist or belongs to another user: `DeckNotFoundException`, 404.
- Malformed data: `InvalidDeckException` / `InvalidCollectionException`, 400.

### Persistence

- `owned_packs (user_id, pack_code)`, primary key on both.
- `decks (id, owner_id, name, hero_code, created_at, updated_at)`, plus
  `deck_aspects (deck_id, position, aspect)` and `deck_slots (deck_id, card_code, quantity)`.
  Plain relational tables instead of JSON columns, so the same SQL runs on H2 and PostgreSQL.
- Deleting a user deletes their collection and decks (`ON DELETE CASCADE`).

## Acceptance criteria

- [ ] `GET /api/collection` returns an empty list for a new user.
- [ ] `PUT /api/collection` replaces the whole collection and returns it sorted.
- [ ] Invalid pack codes give 400 and change nothing.
- [ ] Create, get, list (newest change first), update and delete decks.
- [ ] Malformed deck data gives 400 with the reason.
- [ ] User A cannot read, update or delete user B's deck (404), and lists only their own decks.
- [ ] Users only see their own collection.
- [ ] Domain, service, persistence (H2 and PostgreSQL), web and ownership tests.
- [ ] `./gradlew test` passes.
