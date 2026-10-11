package dev.marlondvg.mc_deck_api.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CollectionRequest(@NotNull @Size(max = 500) List<String> packs) {
}
