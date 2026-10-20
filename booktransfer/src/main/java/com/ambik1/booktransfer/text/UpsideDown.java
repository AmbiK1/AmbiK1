package com.ambik1.booktransfer.text;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Переворачивает текст «вверх ногами»: порядок символов (и строк) разворачивается,
 * каждая буква заменяется на перевёрнутый аналог.
 * Таблицу можно дополнить файлом config/booktransfer-flip.txt (строки вида а=ɐ).
 */
public final class UpsideDown {
	private static final Map<String, String> MAP = new HashMap<>();

	static {
		String[] pairs = {
				// латиница
				"a", "ɐ", "b", "q", "c", "ɔ", "d", "p", "e", "ǝ", "f", "ɟ", "g", "ƃ", "h", "ɥ", "i", "ᴉ",
				"j", "ɾ", "k", "ʞ", "l", "l", "m", "ɯ", "n", "u", "o", "o", "p", "d", "q", "b", "r", "ɹ",
				"s", "s", "t", "ʇ", "u", "n", "v", "ʌ", "w", "ʍ", "x", "x", "y", "ʎ", "z", "z",
				"A", "∀", "B", "ꓭ", "C", "Ɔ", "D", "ꓷ", "E", "Ǝ", "F", "Ⅎ", "G", "⅁", "H", "H", "I", "I",
				"J", "ſ", "K", "ꓘ", "L", "˥", "M", "W", "N", "N", "O", "O", "P", "Ԁ", "Q", "Ό", "R", "ꓤ",
				"S", "S", "T", "⊥", "U", "∩", "V", "Λ", "W", "M", "X", "X", "Y", "⅄", "Z", "Z",
				// кириллица
				"а", "ɐ", "б", "ƍ", "в", "ʚ", "г", "ɹ", "д", "ɓ", "е", "ǝ", "ё", "ǝ", "ж", "ж", "з", "ε",
				"и", "и", "й", "ņ", "к", "ʞ", "л", "v", "м", "w", "н", "н", "о", "о", "п", "u", "р", "d",
				"с", "ɔ", "т", "ɯ", "у", "ʎ", "ф", "ф", "х", "х", "ц", "ǹ", "ч", "Һ", "ш", "m", "щ", "m",
				"ъ", "q", "ы", "ıq", "ь", "q", "э", "є", "ю", "oı", "я", "ʁ",
				"А", "∀", "Б", "ƍ", "В", "ꓭ", "Г", "⅃", "Д", "ɓ", "Е", "Ǝ", "Ё", "Ǝ", "Ж", "Ж", "З", "Ɛ",
				"И", "И", "Й", "Ņ", "К", "ꓘ", "Л", "V", "М", "W", "Н", "Н", "О", "О", "П", "∩", "Р", "Ԁ",
				"С", "Ɔ", "Т", "⊥", "У", "⅄", "Ф", "Ф", "Х", "Х", "Ц", "ǹ", "Ч", "Һ", "Ш", "M", "Щ", "M",
				"Ъ", "q", "Ы", "ıq", "Ь", "q", "Э", "Є", "Ю", "Oı", "Я", "ʁ",
				// цифры
				"0", "0", "1", "Ɩ", "2", "ᄅ", "3", "Ɛ", "4", "ㄣ", "5", "ϛ", "6", "9", "7", "ㄥ", "8", "8", "9", "6",
				// знаки
				".", "˙", ",", "'", "'", ",", "\"", "„", "!", "¡", "?", "¿", "(", ")", ")", "(",
				"[", "]", "]", "[", "{", "}", "}", "{", "<", ">", ">", "<", "_", "‾", "&", "⅋", ";", "؛",
				"«", "»", "»", "«",
		};
		for (int i = 0; i < pairs.length; i += 2) {
			MAP.put(pairs[i], pairs[i + 1]);
		}
	}

	private UpsideDown() {
	}

	/** Подгружает пользовательские замены (файл необязателен). */
	public static void loadOverrides(Path file) {
		if (!Files.isRegularFile(file)) return;
		try {
			for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
				if (line.isBlank() || line.startsWith("#")) continue;
				int eq = line.indexOf('=', 1);
				if (eq <= 0) continue;
				MAP.put(line.substring(0, eq), line.substring(eq + 1));
			}
		} catch (IOException ignored) {
			// Не удалось прочитать — остаёмся со встроенной таблицей.
		}
	}

	public static String flip(String s) {
		StringBuilder out = new StringBuilder(s.length());
		int i = s.length();
		while (i > 0) {
			int cp = s.codePointBefore(i);
			i -= Character.charCount(cp);
			String ch = new String(Character.toChars(cp));
			out.append(MAP.getOrDefault(ch, ch));
		}
		return out.toString();
	}

	/** Переворачивает весь размеченный текст целиком, сохраняя стили кусков. */
	public static List<StyledRun> flip(List<StyledRun> runs) {
		List<StyledRun> out = new ArrayList<>(runs.size());
		for (StyledRun run : runs) {
			out.add(new StyledRun(flip(run.text()), run.style()));
		}
		Collections.reverse(out);
		return out;
	}
}
