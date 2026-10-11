package dev.marlondvg.mc_deck_api.infrastructure.web;

import dev.marlondvg.mc_deck_api.application.port.in.GetCollectionUseCase;
import dev.marlondvg.mc_deck_api.application.port.in.ReplaceCollectionUseCase;
import dev.marlondvg.mc_deck_api.infrastructure.security.CurrentUserId;
import dev.marlondvg.mc_deck_api.infrastructure.web.dto.CollectionRequest;
import dev.marlondvg.mc_deck_api.infrastructure.web.dto.CollectionResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/collection")
class CollectionController {

	private final GetCollectionUseCase getCollection;
	private final ReplaceCollectionUseCase replaceCollection;

	CollectionController(GetCollectionUseCase getCollection, ReplaceCollectionUseCase replaceCollection) {
		this.getCollection = getCollection;
		this.replaceCollection = replaceCollection;
	}

	@GetMapping
	CollectionResponse get(@CurrentUserId UUID ownerId) {
		return CollectionResponse.from(getCollection.getCollection(ownerId));
	}

	@PutMapping
	CollectionResponse replace(@CurrentUserId UUID ownerId, @Valid @RequestBody CollectionRequest request) {
		return CollectionResponse.from(replaceCollection.replaceCollection(ownerId, request.packs()));
	}
}
