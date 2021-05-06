package com.jslib.commons.cli;

import static java.lang.String.format;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import js.util.Files;

public class TextReplace {
	private final List<String> excludes = new ArrayList<>();
	private Predicate<File> filter = file -> true;

	public void addExclude(String exclude) {
		excludes.add(exclude);
	}

	public void setFileExtension(String fileExtension) {
		this.filter = file -> file.getPath().endsWith("." + fileExtension);
	}

	public void setFilter(Predicate<File> filter) {
		this.filter = filter;
	}

	public void replaceAll(File dir, String pattern, String replacement) throws IOException {
		File[] files = dir.listFiles();
		if (files == null) {
			throw new IOException(format("Fail to list files for directory %s.", dir));
		}
		for (File file : files) {
			if (file.isDirectory()) {
				replaceAll(file, pattern, replacement);
				continue;
			}
			if (!filter.test(file)) {
				continue;
			}
			replace(file, pattern, replacement);
		}
	}

	public void replace(File file, String pattern, String replacement) throws IOException {
		File workingFile = new File(file.getParentFile(), '~' + file.getName());
		Reader reader = new FileReader(file);
		Writer writer = new ReplaceWriter(new FileWriter(workingFile), pattern, replacement);
		try {
			Files.copy(reader, writer);
			file.delete();
			workingFile.renameTo(file);
		} finally {
			if (file.exists()) {
				// ensure working file is deleted; ignore delete error
				workingFile.delete();
			}
		}
	}

	static class ReplaceWriter extends Writer {
		private final Writer writer;
		private final char[] pattern;
		private final char[] replacement;

		private int patternIndex;

		public ReplaceWriter(Writer writer, String pattern, String replacement) {
			super();
			this.writer = writer;
			this.pattern = pattern.toCharArray();
			this.replacement = replacement.toCharArray();
		}

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			for (int i = 0; i < len; ++i) {
				char c = cbuf[off + i];

				if (c == pattern[patternIndex]) {
					++patternIndex;
					if (patternIndex == pattern.length) {
						patternIndex = 0;
						writer.write(replacement, 0, replacement.length);
					}
				} else {
					if (patternIndex > 0) {
						writer.write(pattern, 0, patternIndex);
						patternIndex = 0;
					}
					writer.write(c);
				}
			}
		}

		@Override
		public void flush() throws IOException {
			if (patternIndex > 0) {
				writer.write(pattern, 0, patternIndex);
			}
			writer.flush();
		}

		@Override
		public void close() throws IOException {
			writer.close();
		}
	}
}
