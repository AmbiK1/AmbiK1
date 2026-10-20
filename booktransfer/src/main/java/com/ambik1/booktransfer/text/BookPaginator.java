package com.ambik1.booktransfer.text;

import java.util.ArrayList;
import java.util.List;

/**
 * Раскладывает текст по страницам книги.
 * Слова не разрываются (кроме слов длиннее строки), каждая страница начинается
 * с повторения активного форматирования, пробелы в начале страницы отбрасываются.
 * Отдельная строка «---» — принудительный переход на новую страницу.
 */
public final class BookPaginator {
	public static final int MAX_PAGE_CHARS = 1024;
	public static final int MAX_LINES = 14;
	public static final String PAGE_BREAK = "---";

	/** Сколько строк займёт текст страницы (с §-кодами) в книге. */
	@FunctionalInterface
	public interface LineCounter {
		int lines(String pageText);
	}

	private enum Kind { WORD, SPACE, NEWLINE, PAGE_BREAK }

	/** style — стиль в начале токена, endStyle — в конце (слово может менять стиль внутри). */
	private record Token(Kind kind, String text, TextStyle style, TextStyle endStyle) {
		Token(Kind kind, String text, TextStyle style) {
			this(kind, text, style, style);
		}
	}

	private final LineCounter counter;
	private final int maxLines;
	private final int maxChars;

	public BookPaginator(LineCounter counter) {
		this(counter, MAX_LINES, MAX_PAGE_CHARS);
	}

	public BookPaginator(LineCounter counter, int maxLines, int maxChars) {
		this.counter = counter;
		this.maxLines = maxLines;
		this.maxChars = maxChars;
	}

	public List<String> paginate(List<StyledRun> runs) {
		List<String> pages = new ArrayList<>();
		Page page = new Page();
		for (Token token : tokenize(runs)) {
			switch (token.kind) {
				case PAGE_BREAK -> {
					pages.add(page.text());
					page = new Page();
				}
				case SPACE, NEWLINE -> {
					// Пробелы и переносы в начале страницы, появившиеся из-за автопереноса, не нужны.
					if (page.isEmpty() && page.automatic) continue;
					if (!page.tryAdd(token)) {
						pages.add(page.text());
						page = new Page();
						page.automatic = true;
					}
				}
				case WORD -> {
					if (!page.tryAdd(token)) {
						// Короткое слово переносим на новую страницу целиком,
						// а слово длиннее строки режем прямо на текущей.
						boolean longerThanLine = counter.lines(token.text) > 1;
						if (!page.isEmpty() && !longerThanLine) {
							pages.add(page.text());
							page = new Page();
							page.automatic = true;
						}
						// Слово не влезает даже на пустую страницу — режем по символам.
						for (Token piece : splitWord(token)) {
							if (!page.tryAdd(piece)) {
								pages.add(page.text());
								page = new Page();
								page.automatic = true;
								page.forceAdd(piece);
							}
						}
					}
				}
			}
		}
		if (!page.isEmpty() || pages.isEmpty()) {
			pages.add(page.text());
		}
		// Убираем хвостовые пустые страницы.
		while (pages.size() > 1 && pages.get(pages.size() - 1).isBlank()) {
			pages.remove(pages.size() - 1);
		}
		return pages;
	}

	private List<Token> splitWord(Token word) {
		List<Token> pieces = new ArrayList<>();
		String s = word.text;
		TextStyle style = word.style;
		int i = 0;
		while (i < s.length()) {
			int start = i;
			TextStyle startStyle = style;
			// §-коды внутри слова прилипают к следующему символу.
			while (i + 1 < s.length() && s.charAt(i) == TextStyle.SECTION) {
				style = applyCode(style, s.charAt(i + 1));
				i += 2;
			}
			if (i < s.length()) i += Character.charCount(s.codePointAt(i));
			String piece = s.substring(start, i);
			pieces.add(new Token(Kind.WORD, piece, startStyle, style));
		}
		return pieces;
	}

	private static TextStyle applyCode(TextStyle style, char code) {
		if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) return style.withColor(code);
		if (code == 'r') return TextStyle.PLAIN;
		return style.with(code);
	}

	private final class Page {
		private final StringBuilder sb = new StringBuilder();
		private TextStyle current = TextStyle.PLAIN;
		private boolean hasContent;
		boolean automatic;

		boolean isEmpty() {
			return !hasContent;
		}

		String text() {
			// Висячие пробелы/переносы в конце страницы не нужны.
			String s = sb.toString();
			int end = s.length();
			while (end > 0 && (s.charAt(end - 1) == ' ' || s.charAt(end - 1) == '\n')) end--;
			return s.substring(0, end);
		}

		boolean tryAdd(Token token) {
			String addition = additionFor(token);
			int before = sb.length();
			sb.append(addition);
			if (sb.length() <= maxChars && counter.lines(sb.toString()) <= maxLines) {
				commit(token);
				return true;
			}
			sb.setLength(before);
			return false;
		}

		void forceAdd(Token token) {
			sb.append(additionFor(token));
			commit(token);
		}

		private String additionFor(Token token) {
			String text = token.kind == Kind.NEWLINE ? "\n" : token.text;
			// У пробелов/переносов стиль важен только для подчёркивания/зачёркивания.
			TextStyle target = token.style;
			if (token.kind == Kind.NEWLINE) target = current;
			return target.transitionFrom(current) + text;
		}

		private void commit(Token token) {
			if (token.kind != Kind.NEWLINE) current = token.endStyle;
			hasContent = true;
		}
	}

	private static List<Token> tokenize(List<StyledRun> runs) {
		List<Token> tokens = new ArrayList<>();
		// Сначала проверяем строки-разделители страниц по «чистому» тексту.
		for (StyledRun run : runs) {
			String s = run.text();
			int i = 0;
			while (i < s.length()) {
				char c = s.charAt(i);
				if (c == '\n') {
					tokens.add(new Token(Kind.NEWLINE, "\n", run.style()));
					i++;
				} else if (c == ' ') {
					int j = i;
					while (j < s.length() && s.charAt(j) == ' ') j++;
					tokens.add(new Token(Kind.SPACE, s.substring(i, j), run.style()));
					i = j;
				} else {
					int j = i;
					while (j < s.length() && s.charAt(j) != ' ' && s.charAt(j) != '\n') j++;
					appendWord(tokens, s.substring(i, j), run.style());
					i = j;
				}
			}
		}
		return markPageBreaks(tokens);
	}

	/** Слово, разбитое сменой стиля (например **жир**ный), должно остаться одним целым. */
	private static void appendWord(List<Token> tokens, String text, TextStyle style) {
		if (!tokens.isEmpty()) {
			Token last = tokens.get(tokens.size() - 1);
			if (last.kind == Kind.WORD) {
				tokens.set(tokens.size() - 1, new Token(Kind.WORD,
						last.text + style.transitionFrom(last.endStyle) + text, last.style, style));
				return;
			}
		}
		tokens.add(new Token(Kind.WORD, text, style));
	}

	private static List<Token> markPageBreaks(List<Token> tokens) {
		List<Token> out = new ArrayList<>(tokens.size());
		for (int i = 0; i < tokens.size(); i++) {
			Token t = tokens.get(i);
			boolean lineStart = i == 0 || tokens.get(i - 1).kind == Kind.NEWLINE;
			boolean lineEnd = i + 1 == tokens.size() || tokens.get(i + 1).kind == Kind.NEWLINE;
			if (t.kind == Kind.WORD && lineStart && lineEnd && t.text.equals(PAGE_BREAK)) {
				// Убираем перенос строки перед разделителем и после него.
				if (!out.isEmpty() && out.get(out.size() - 1).kind == Kind.NEWLINE) out.remove(out.size() - 1);
				out.add(new Token(Kind.PAGE_BREAK, "", t.style));
				if (i + 1 < tokens.size()) i++;
				continue;
			}
			out.add(t);
		}
		return out;
	}
}
