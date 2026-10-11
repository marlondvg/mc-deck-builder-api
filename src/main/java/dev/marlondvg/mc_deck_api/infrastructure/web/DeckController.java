package dev.marlondvg.mc_deck_api.infrastructure.web;

import dev.marlondvg.mc_deck_api.application.port.in.CreateDeckUseCase;
import dev.marlondvg.mc_deck_api.application.port.in.DeleteDeckUseCase;
import dev.marlondvg.mc_deck_api.application.port.in.GetDeckUseCase;
import dev.marlondvg.mc_deck_api.application.port.in.ListDecksUseCase;
import dev.marlondvg.mc_deck_api.application.port.in.UpdateDeckUseCase;
import dev.marlondvg.mc_deck_api.domain.Deck;
import dev.marlondvg.mc_deck_api.infrastructure.security.CurrentUserId;
import dev.marlondvg.mc_deck_api.infrastructure.web.dto.DeckRequest;
import dev.marlondvg.mc_deck_api.infrastructure.web.dto.DeckResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/decks")
class DeckController {

	private final CreateDeckUseCase createDeck;
	private final GetDeckUseCase getDeck;
	private final ListDecksUseCase listDecks;
	private final UpdateDeckUseCase updateDeck;
	private final DeleteDeckUseCase deleteDeck;

	DeckController(CreateDeckUseCase createDeck, GetDeckUseCase getDeck, ListDecksUseCase listDecks,
			UpdateDeckUseCase updateDeck, DeleteDeckUseCase deleteDeck) {
		this.createDeck = createDeck;
		this.getDeck = getDeck;
		this.listDecks = listDecks;
		this.updateDeck = updateDeck;
		this.deleteDeck = deleteDeck;
	}

	@PostMapping
	ResponseEntity<DeckResponse> create(@CurrentUserId UUID ownerId, @Valid @RequestBody DeckRequest request) {
		Deck deck = createDeck.createDeck(ownerId, request.toContents());
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(deck.getId())
				.toUri();
		return ResponseEntity.created(location).body(DeckResponse.from(deck));
	}

	@GetMapping
	List<DeckResponse> list(@CurrentUserId UUID ownerId) {
		return listDecks.listDecks(ownerId).stream().map(DeckResponse::from).toList();
	}

	@GetMapping("/{id}")
	DeckResponse get(@CurrentUserId UUID ownerId, @PathVariable UUID id) {
		return DeckResponse.from(getDeck.getDeck(ownerId, id));
	}

	@PutMapping("/{id}")
	DeckResponse update(@CurrentUserId UUID ownerId, @PathVariable UUID id, @Valid @RequestBody DeckRequest request) {
		return DeckResponse.from(updateDeck.updateDeck(ownerId, id, request.toContents()));
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> delete(@CurrentUserId UUID ownerId, @PathVariable UUID id) {
		deleteDeck.deleteDeck(ownerId, id);
		return ResponseEntity.noContent().build();
	}
}
