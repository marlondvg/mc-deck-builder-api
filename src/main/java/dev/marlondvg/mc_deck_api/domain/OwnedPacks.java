package dev.marlondvg.mc_deck_api.domain;

import dev.marlondvg.mc_deck_api.domain.exception.InvalidCollectionException;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * The packs a user owns, by MarvelCDB pack code (e.g. {@code core}, {@code mut_gen}).
 */
public final class OwnedPacks {

	public static final int MAX_PACKS = 500;
	private static final Pattern PACK_CODE = Pattern.compile("^[a-z0-9_]{1,50}$");

	private final SortedSet<String> codes;

	private OwnedPacks(SortedSet<String> codes) {
		this.codes = Collections.unmodifiableSortedSet(codes);
	}

	public static OwnedPacks empty() {
		return new OwnedPacks(new TreeSet<>());
	}

	/**
	 * Validates the codes; duplicates collapse.
	 */
	public static OwnedPacks of(Collection<String> codes) {
		if (codes == null) {
			throw new InvalidCollectionException("packs are required");
		}
		SortedSet<String> unique = new TreeSet<>();
		for (String code : codes) {
			if (code == null || !PACK_CODE.matcher(code).matches()) {
				throw new InvalidCollectionException("invalid pack code: " + code);
			}
			unique.add(code);
		}
		if (unique.size() > MAX_PACKS) {
			throw new InvalidCollectionException("at most " + MAX_PACKS + " packs");
		}
		return new OwnedPacks(unique);
	}

	/**
	 * Pack codes in alphabetical order.
	 */
	public SortedSet<String> codes() {
		return codes;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof OwnedPacks other && codes.equals(other.codes);
	}

	@Override
	public int hashCode() {
		return codes.hashCode();
	}

	@Override
	public String toString() {
		return "OwnedPacks" + codes;
	}
}
