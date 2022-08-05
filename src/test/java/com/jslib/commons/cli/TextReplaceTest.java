package com.jslib.commons.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.Test;

import com.jslib.util.Files;

public class TextReplaceTest {
	@Test
	public void replace() throws IOException {
		// given
		Reader reader = new StringReader("<div w:compo='lib/captcha'>lib captcha or res/compo/captcha</div>");
		StringWriter string = new StringWriter();
		Writer writer = new TextReplace.ReplaceWriter(string, "lib/captcha", "res/compo/captcha");

		// when
		Files.copy(reader, writer);

		// then
		assertThat(string.toString(), equalTo("<div w:compo='res/compo/captcha'>lib captcha or res/compo/captcha</div>"));
	}
}
