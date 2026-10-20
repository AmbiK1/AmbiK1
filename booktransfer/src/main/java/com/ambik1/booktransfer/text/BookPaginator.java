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
	public static final int PAGE_WIDTH = 114;
	public static final String PAGE_BREAK = "---";
	/** Запас символов под номер страницы. */
	private static final int NUMBER_RESERVE = 48;

	/** Как книга меряет текст (с §-кодами). */
	public interface Measurer {
		/** Сколько строк займёт текст страницы. */
		int lines(String pageText);

		/** Ширина одной строки в пикселях. */
		int width(String text);
	}

	/** Настройки раскладки. */
	public record Options(boolean pageNumbers, boolean tableOfContents, boolean chapterNewPage,
						  int pageOffset, String tocTitle) {
		public static final Options DEFAULT = new Options(false, false, true, 0, "Contents");
	}

	private enum Kind { WORD, SPACE, PAD, NEWLINE, PAGE_BREAK, HEADING }

	/** style — стиль в начале токена, endStyle — в конце (слово может менять стиль внутри). */
	private record Token(Kind kind, String text, TextStyle style, TextStyle endStyle) {
		Token(Kind kind, String text, TextStyle style) {
			this(kind, text, style, style);
		}
	}

	private record Paragraph(List<StyledRun> runs, StyledRun.Kind kind) {
		String plain() {
			StringBuilder sb = new StringBuilder();
			runs.forEach(r -> sb.append(r.text()));
			return sb.toString();
		}
	}

	private record TocEntry(String title, int bodyPage) {
	}

	private final Measurer measurer;
	private final Options options;
	private final int maxLines;
	private final int maxChars;
	private final List<TocEntry> toc = new ArrayList<>();

	public BookPaginator(Measurer measurer) {
		this(measurer, Options.DEFAULT);
	}

	public BookPaginator(Measurer measurer, Options options) {
		this.measurer = measurer;
		this.options = options;
		this.maxLines = options.pageNumbers() ? MAX_LINES - 1 : MAX_LINES;
		this.maxChars = options.pageNumbers() ? MAX_PAGE_CHARS - NUMBER_RESERVE : MAX_PAGE_CHARS;
	}

	public List<String> paginate(List<StyledRun> runs) {
		toc.clear();
		List<String> body = layout(tokenize(runs));

		List<String> all = new ArrayList<>();
		if (options.tableOfContents() && !toc.isEmpty()) {
			all.addAll(buildToc());
		}
		all.addAll(body);
		if (options.pageNumbers()) {
			for (int i = 0; i < all.size(); i++) {
				all.set(i, withNumber(all.get(i), options.pageOffset() + i + 1));
			}
		}
		return all;
	}

	// ---------- раскладка по страницам ----------

	private List<String> layout(List<Token> tokens) {
		List<String> pages = new ArrayList<>();
		Page page = new Page();
		for (Token token : tokens) {
			switch (token.kind) {
				case PAGE_BREAK -> {
					pages.add(page.text());
					page = new Page();
				}
				case HEADING -> {
					if (options.chapterNewPage() && !page.isEmpty()) {
						pages.add(page.text());
						page = new Page();
						page.automatic = true;
					}
					toc.add(new TocEntry(token.text, pages.size()));
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
				case PAD -> {
					if (!page.tryAdd(token)) {
						pages.add(page.text());
						page = new Page();
						page.automatic = true;
						page.forceAdd(token);
					}
				}
				case WORD -> {
					if (page.tryAdd(token)) continue;
					// Короткое слово переносим на новую страницу целиком,
					// а слово длиннее строки режем прямо на текущей.
					boolean longerThanLine = measurer.lines(token.text) > 1;
					if (!page.isEmpty() && !longerThanLine) {
						pages.add(page.text());
						page = new Page();
						page.automatic = true;
						if (page.tryAdd(token)) continue;
					}
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
				style = style.apply(s.charAt(i + 1));
				i += 2;
			}
			if (i < s.length()) i += Character.charCount(s.codePointAt(i));
			pieces.add(new Token(Kind.WORD, s.substring(start, i), startStyle, style));
		}
		return pieces;
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
			int before = sb.length();
			sb.append(additionFor(token));
			if (sb.length() <= maxChars && measurer.lines(sb.toString()) <= maxLines) {
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
			if (token.kind == Kind.NEWLINE) return "\n";
			return token.style.transitionFrom(current) + token.text;
		}

		private void commit(Token token) {
			if (token.kind != Kind.NEWLINE) current = token.endStyle;
			hasContent = true;
		}
	}

	// ---------- разбор на токены ----------

	private List<Token> tokenize(List<StyledRun> runs) {
		List<Paragraph> paragraphs = splitParagraphs(runs);
		List<Token> tokens = new ArrayList<>();
		boolean skipNewline = true;
		for (Paragraph p : paragraphs) {
			if (p.kind == StyledRun.Kind.NORMAL && p.plain().trim().equals(PAGE_BREAK)) {
				if (!tokens.isEmpty() && tokens.get(tokens.size() - 1).kind == Kind.NEWLINE) {
					tokens.remove(tokens.size() - 1);
				}
				tokens.add(new Token(Kind.PAGE_BREAK, "", TextStyle.PLAIN));
				skipNewline = true;
				continue;
			}
			if (!skipNewline) tokens.add(new Token(Kind.NEWLINE, "\n", TextStyle.PLAIN));
			skipNewline = false;

			switch (p.kind) {
				case NORMAL -> appendWords(tokens, p.runs);
				case CENTER -> appendCentered(tokens, p.runs);
				case HEADING -> {
					String title = p.plain().trim();
					if (!title.isEmpty()) tokens.add(new Token(Kind.HEADING, title, TextStyle.PLAIN));
					appendCentered(tokens, p.runs);
				}
			}
		}
		return tokens;
	}

	/** Делит текст на абзацы по переводам строк; тип абзаца — по первому непустому куску. */
	private static List<Paragraph> splitParagraphs(List<StyledRun> runs) {
		List<Paragraph> result = new ArrayList<>();
		List<StyledRun> current = new ArrayList<>();
		for (StyledRun run : runs) {
			String[] parts = run.text().split("\n", -1);
			for (int i = 0; i < parts.length; i++) {
				if (i > 0) {
					result.add(paragraph(current));
					current = new ArrayList<>();
				}
				if (!parts[i].isEmpty()) current.add(new StyledRun(parts[i], run.style(), run.kind()));
			}
		}
		result.add(paragraph(current));
		return result;
	}

	private static Paragraph paragraph(List<StyledRun> runs) {
		StyledRun.Kind kind = StyledRun.Kind.NORMAL;
		for (StyledRun r : runs) {
			if (!r.text().isBlank()) {
				kind = r.kind();
				break;
			}
		}
		return new Paragraph(runs, kind);
	}

	private static List<Token> words(List<StyledRun> runs) {
		List<Token> tokens = new ArrayList<>();
		for (StyledRun run : runs) {
			String s = run.text();
			int i = 0;
			while (i < s.length()) {
				if (s.charAt(i) == ' ') {
					int j = i;
					while (j < s.length() && s.charAt(j) == ' ') j++;
					tokens.add(new Token(Kind.SPACE, s.substring(i, j), run.style()));
					i = j;
				} else {
					int j = i;
					while (j < s.length() && s.charAt(j) != ' ') j++;
					appendWord(tokens, s.substring(i, j), run.style());
					i = j;
				}
			}
		}
		return tokens;
	}

	private static void appendWords(List<Token> out, List<StyledRun> runs) {
		out.addAll(words(runs));
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

	/** Сам переносит абзац по строкам и добавляет перед каждой строкой пробелы для центровки. */
	private void appendCentered(List<Token> out, List<StyledRun> runs) {
		List<Token> words = words(runs);
		List<List<Token>> lines = new ArrayList<>();
		List<Token> line = new ArrayList<>();
		for (Token t : words) {
			if (t.kind == Kind.SPACE) {
				if (!line.isEmpty()) line.add(t);
				continue;
			}
			List<Token> candidate = new ArrayList<>(line);
			candidate.add(t);
			if (!line.isEmpty() && lineWidth(candidate) > PAGE_WIDTH) {
				lines.add(trimTrailingSpaces(line));
				line = new ArrayList<>();
			}
			line.add(t);
		}
		if (!line.isEmpty()) lines.add(trimTrailingSpaces(line));

		int spaceWidth = Math.max(1, measurer.width(" "));
		for (int i = 0; i < lines.size(); i++) {
			if (i > 0) out.add(new Token(Kind.NEWLINE, "\n", TextStyle.PLAIN));
			List<Token> l = lines.get(i);
			int pad = Math.max(0, (PAGE_WIDTH - lineWidth(l)) / 2 / spaceWidth);
			if (pad > 0) out.add(new Token(Kind.PAD, " ".repeat(pad), TextStyle.PLAIN));
			out.addAll(l);
		}
	}

	private static List<Token> trimTrailingSpaces(List<Token> line) {
		List<Token> copy = new ArrayList<>(line);
		while (!copy.isEmpty() && copy.get(copy.size() - 1).kind == Kind.SPACE) copy.remove(copy.size() - 1);
		return copy;
	}

	private int lineWidth(List<Token> line) {
		StringBuilder sb = new StringBuilder();
		TextStyle current = TextStyle.PLAIN;
		for (Token t : line) {
			sb.append(t.style.transitionFrom(current)).append(t.text);
			current = t.endStyle;
		}
		return measurer.width(sb.toString());
	}

	// ---------- оглавление и номера страниц ----------

	private List<String> buildToc() {
		int linesTotal = toc.size() + 2;
		int tocPages = (linesTotal + maxLines - 1) / maxLines;
		List<String> lines = new ArrayList<>();
		String title = TextStyle.SECTION + "l" + options.tocTitle();
		int pad = Math.max(0, (PAGE_WIDTH - measurer.width(title)) / 2 / Math.max(1, measurer.width(" ")));
		lines.add(" ".repeat(pad) + title + TextStyle.SECTION + "r");
		lines.add("");
		for (TocEntry e : toc) {
			int number = options.pageOffset() + tocPages + e.bodyPage + 1;
			lines.add(tocLine(e.title, number));
		}
		List<String> pages = new ArrayList<>();
		for (int i = 0; i < lines.size(); i += maxLines) {
			pages.add(String.join("\n", lines.subList(i, Math.min(lines.size(), i + maxLines))));
		}
		return pages;
	}

	/** «Название ........ 12» в одну строку. */
	private String tocLine(String title, int number) {
		String num = " " + number;
		int dotWidth = Math.max(1, measurer.width("."));
		int available = PAGE_WIDTH - measurer.width(num) - measurer.width(" ") - dotWidth * 2;
		String name = title;
		if (measurer.width(name) > available) {
			while (!name.isEmpty() && measurer.width(name + "…") > available) {
				name = name.substring(0, name.length() - 1);
			}
			name = name + "…";
		}
		int dots = Math.max(0, (PAGE_WIDTH - measurer.width(name + " ") - measurer.width(num)) / dotWidth);
		String line = name + " " + ".".repeat(dots) + num;
		while (dots > 0 && measurer.width(line) > PAGE_WIDTH) {
			dots--;
			line = name + " " + ".".repeat(dots) + num;
		}
		return line;
	}

	private String withNumber(String page, int number) {
		int used = page.isEmpty() ? 0 : Math.max(1, measurer.lines(page));
		String num = TextStyle.SECTION + "8" + number;
		int pad = Math.max(0, (PAGE_WIDTH - measurer.width(num)) / 2 / Math.max(1, measurer.width(" ")));
		String prefix = page.isEmpty() ? "" : page + TextStyle.SECTION + "r";
		return prefix + "\n".repeat(Math.max(1, MAX_LINES - used) - (page.isEmpty() ? 1 : 0))
				+ " ".repeat(pad) + num;
	}
}
