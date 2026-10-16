package com.ninjachicken784.advancednucruns;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Every item a Crystal Nucleus run can give (crystals are left out), with the names the game
 * uses for prices and the XP each one needs on the RNG meter. Contains no Minecraft code so it
 * can be tested on its own.
 */
public final class NucleusItems {
	/** How an item is coloured on the tracker. Jaderald is purple, Jade Dye is green, the rest are blue. */
	public enum Style { BLUE, PURPLE, GREEN }

	/**
	 * One drop.
	 *
	 * @param key      stable name used in the save file
	 * @param name     name shown on screen and used in chat
	 * @param priceId  Skyblock item id used for the Bazaar and the Auction House
	 * @param binId    id used by the lowest BIN list when it is not the same as priceId (or null)
	 * @param meterXp  Nucleus XP this item needs on the RNG meter, or 0 if it is not on the meter
	 * @param aliases  other ways the name can be written in chat
	 */
	public record Item(String key, String name, String priceId, String binId, Style style, long meterXp,
					   List<String> aliases) { }

	/** One of the things a run costs. The name is used to find the price if the id is not listed. */
	public record RunCost(String id, String name) { }

	/** The three things a run costs: one of each, bought at the Bazaar. */
	public static final List<RunCost> RUN_COSTS = List.of(
		new RunCost("PRECURSOR_APPARATUS", "Precursor Apparatus"),
		new RunCost("GOBLIN_EGG", "Goblin Egg"),
		new RunCost("JUNGLE_KEY", "Jungle Key"));

	/** All drops, in the order they are listed in the tracker GUI. */
	public static final List<Item> ALL = build();

	private NucleusItems() { }

	private static List<Item> build() {
		List<Item> items = new ArrayList<>();

		// Flawless gemstones.
		for (String gem : new String[] {"Jasper", "Ruby", "Amethyst", "Jade", "Amber", "Sapphire", "Topaz"}) {
			items.add(gem("Flawless", gem));
		}

		items.add(item("DIVANS_ALLOY", "Divan's Alloy", "DIVAN_ALLOY", null, Style.BLUE, 1_000_000L));
		items.add(item("QUICK_CLAW", "Quick Claw", "PET_ITEM_QUICK_CLAW", null, Style.BLUE, 450_000L));
		items.add(gem("Fine", "Jasper"));
		items.add(item("TREASURITE", "Treasurite", "TREASURITE", null, Style.BLUE, 0L));
		items.add(item("RECALL_POTION", "Recall Potion", "RECALL_POTION", null, Style.BLUE, 100_000L));
		items.add(item("WISHING_COMPASS", "Wishing Compass", "WISHING_COMPASS", null, Style.BLUE, 0L));
		items.add(item("PRECIOUS_PEARL", "Precious Pearl", "PRECIOUS_PEARL", null, Style.BLUE, 35_000L));
		items.add(item("CLAW_FOSSIL", "Claw Fossil", "CLAW_FOSSIL", null, Style.BLUE, 50_000L));
		items.add(gem("Flawed", "Jasper"));
		items.add(item("GEMSTONE_MIXTURE", "Gemstone Mixture", "GEMSTONE_MIXTURE", null, Style.BLUE, 35_000L));
		items.add(item("BOOK_FORTUNE_4", "Enchanted Book (Fortune IV)", "ENCHANTMENT_FORTUNE_4", "FORTUNE;4",
			Style.BLUE, 35_000L, "Fortune IV", "Enchanted Book Fortune IV", "Enchanted Book (Fortune 4)"));
		items.add(item("GOLD_ESSENCE", "Gold Essence", "ESSENCE_GOLD", null, Style.BLUE, 0L));
		items.add(item("DIAMOND_ESSENCE", "Diamond Essence", "ESSENCE_DIAMOND", null, Style.BLUE, 0L));

		for (String gem : new String[] {"Ruby", "Amethyst", "Jade", "Amber", "Sapphire", "Topaz"}) {
			items.add(gem("Fine", gem));
		}

		items.add(item("JADERALD", "Jaderald", "JADERALD", null, Style.PURPLE, 20_000L));
		items.add(item("DWARVEN_OS_GRAHAMS", "Dwarven O's Gemstone Grahams", "DWARVEN_OS_GEMSTONE_GRAHAMS", null,
			Style.BLUE, 20_000L, "Dwarven Os Gemstone Grahams"));

		for (String gem : new String[] {"Ruby", "Amethyst", "Jade", "Amber", "Sapphire", "Topaz"}) {
			items.add(gem("Flawed", gem));
		}

		items.add(item("HELIX_FOSSIL", "Helix Fossil", "HELIX", null, Style.BLUE, 10_000L));
		items.add(item("BOOK_LAPIDARY_1", "Enchanted Book (Lapidary I)", "ENCHANTMENT_LAPIDARY_1", "LAPIDARY;1",
			Style.BLUE, 10_000L, "Lapidary I", "Enchanted Book Lapidary I", "Enchanted Book (Lapidary 1)"));
		items.add(item("PREHISTORIC_EGG", "Prehistoric Egg", "PREHISTORIC_EGG", null, Style.BLUE, 8_000L));
		items.add(item("DIVAN_FRAGMENT", "Divan Fragment", "DIVAN_FRAGMENT", null, Style.BLUE, 8_000L));
		items.add(item("OIL_BARREL", "Oil Barrel", "OIL_BARREL", null, Style.BLUE, 0L));
		items.add(item("PICKONIMBUS", "Pickonimbus 2000", "PICKONIMBUS", null, Style.BLUE, 4_000L,
			"Pickonimbus"));
		items.add(item("BOB_OMB", "Bob-omb", "BOB_OMB", null, Style.BLUE, 0L));
		items.add(item("JADE_DYE", "Jade Dye", "DYE_JADE", null, Style.GREEN, 5_000_000L));

		return List.copyOf(items);
	}

	/** A gemstone such as "Fine Ruby Gemstone", whose id is FINE_RUBY_GEM. */
	private static Item gem(String grade, String gem) {
		String upperGrade = grade.toUpperCase(Locale.ROOT);
		String upperGem = gem.toUpperCase(Locale.ROOT);
		return item(upperGrade + "_" + upperGem, grade + " " + gem + " Gemstone",
			upperGrade + "_" + upperGem + "_GEM", null, Style.BLUE, 0L);
	}

	private static Item item(String key, String name, String priceId, String binId, Style style, long meterXp,
							 String... aliases) {
		return new Item(key, name, priceId, binId, style, meterXp, List.of(aliases));
	}

	/** Lower case with only letters and digits, so "Divan's Alloy" and "divans alloy!" match. */
	public static String normalize(String text) {
		if (text == null) {
			return "";
		}
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (Character.isLetterOrDigit(c)) {
				out.append(Character.toLowerCase(c));
			}
		}
		return out.toString();
	}

	/** The drop with this name (or one of its aliases), or null if it is not a known drop. */
	public static Item find(String name) {
		String wanted = normalize(name);
		if (wanted.isEmpty()) {
			return null;
		}
		for (Item item : ALL) {
			if (normalize(item.name()).equals(wanted)) {
				return item;
			}
			for (String alias : item.aliases()) {
				if (normalize(alias).equals(wanted)) {
					return item;
				}
			}
		}
		return null;
	}

	/** The drop with this save-file key, or null. */
	public static Item byKey(String key) {
		for (Item item : ALL) {
			if (item.key().equals(key)) {
				return item;
			}
		}
		return null;
	}
}
