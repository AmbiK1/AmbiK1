package com.ambik1.booktransfer.text;

import java.util.ArrayList;
import java.util.List;

/**
 * Разбирает разметку пользователя:
 * <ul>
 *   <li>коды Minecraft через &amp;: &amp;l жирный, &amp;o курсив, &amp;m зачёркнутый, &amp;n подчёркнутый,
 *       &amp;k меняющийся, &amp;r сброс, &amp;0-&amp;f цвета; &amp;&amp; — сам символ &amp;;</li>
 *   <li>markdown: **жирный**, *курсив*, ~~зачёркнутый~~, __подчёркнутый__, ||меняющийся||;</li>
 *   <li>\ перед символом отменяет его особый смысл.</li>
 * </ul>
 */
public final class MarkupParser {
	private MarkupParser() {
	}

	public static List<StyledRun> parse(String input, boolean formatting) {
		List<StyledRun> runs = new ArrayList<>();
		String text = normalize(input);
		if (!formatting) {
			if (!text.isEmpty()) runs.add(new StyledRun(text, TextStyle.PLAIN));
			return runs;
		}

		TextStyle style = TextStyle.PLAIN;
		StringBuilder buf = new StringBuilder();
		int i = 0;
		int n = text.length();
		while (i < n) {
			char c = text.charAt(i);
			char next = i + 1 < n ? text.charAt(i + 1) : 0;
			TextStyle newStyle = null;
			int consumed = 0;

			if (c == '\\' && i + 1 < n && "\\*~_|&".indexOf(next) >= 0) {
				buf.append(next);
				i += 2;
				continue;
			}
			if (c == '&' && next == '&') {
				buf.append('&');
				i += 2;
				continue;
			}
			if (c == '&' || c == TextStyle.SECTION) {
				char code = Character.toLowerCase(next);
				if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
					newStyle = style.withColor(code);
					consumed = 2;
				} else if ("klmno".indexOf(code) >= 0 && code != 0) {
					newStyle = style.with(code);
					consumed = 2;
				} else if (code == 'r') {
					newStyle = TextStyle.PLAIN;
					consumed = 2;
				}
			} else if (c == '*' && next == '*') {
				newStyle = style.toggle('l');
				consumed = 2;
			} else if (c == '*') {
				newStyle = style.toggle('o');
				consumed = 1;
			} else if (c == '~' && next == '~') {
				newStyle = style.toggle('m');
				consumed = 2;
			} else if (c == '_' && next == '_') {
				newStyle = style.toggle('n');
				consumed = 2;
			} else if (c == '|' && next == '|') {
				newStyle = style.toggle('k');
				consumed = 2;
			}

			if (newStyle != null) {
				if (!newStyle.equals(style)) {
					flush(runs, buf, style);
					style = newStyle;
				}
				i += consumed;
			} else {
				buf.append(c);
				i++;
			}
		}
		flush(runs, buf, style);
		return runs;
	}

	private static void flush(List<StyledRun> runs, StringBuilder buf, TextStyle style) {
		if (buf.length() > 0) {
			runs.add(new StyledRun(buf.toString(), style));
			buf.setLength(0);
		}
	}

	/** Приводит переводы строк и табы к виду, понятному книге. */
	static String normalize(String s) {
		return s.replace("\r\n", "\n").replace('\r', '\n').replace("\t", "    ")
				.replace(' ', ' ');
	}

	/** Превращает §-коды обратно в &amp;-разметку (для копирования остатка текста). */
	public static String toAmpersand(String s) {
		return s.replace("&", "&&").replace(TextStyle.SECTION, '&');
	}
}
