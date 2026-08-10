package com.gamesup.api.catalog.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class CatalogNameNormalizer {

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private CatalogNameNormalizer() {
	}

	public static String displayName(String value) {
		if (value == null) {
			return null;
		}
		return WHITESPACE.matcher(Normalizer.normalize(value.trim(), Normalizer.Form.NFKC))
				.replaceAll(" ");
	}

	public static String normalizedName(String value) {
		String displayName = displayName(value);
		return displayName == null ? null : displayName.toLowerCase(Locale.ROOT);
	}
}
