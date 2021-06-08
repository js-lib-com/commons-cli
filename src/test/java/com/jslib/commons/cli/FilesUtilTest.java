package com.jslib.commons.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.io.IConsole;
import js.lang.BugError;

@RunWith(MockitoJUnitRunner.class)
public class FilesUtilTest {
	@Mock
	private FileSystemProvider provider;
	@Mock
	private FileSystem fileSystem;
	@Mock
	private IConsole console;

	private FilesUtil files;

	@Before
	public void beforeTest() {
		when(fileSystem.provider()).thenReturn(provider);

		files = new FilesUtil(fileSystem, console);
	}

	@Test
	public void getWorkingDir() {
		// given
		Path workingDir = mock(Path.class);
		when(fileSystem.getPath("")).thenReturn(workingDir);

		Path absoluteDir = mock(Path.class);
		when(workingDir.toAbsolutePath()).thenReturn(absoluteDir);

		// when
		Path dir = files.getWorkingDir();

		// then
		assertThat(dir, equalTo(absoluteDir));
		verify(fileSystem, times(1)).getPath("");
		verify(workingDir, times(1)).toAbsolutePath();
	}

	@Test
	public void getProjectDir() {
		// given
		Path workingDir = mock(Path.class);
		when(fileSystem.getPath("")).thenReturn(workingDir);

		Path absoluteDir = mock(Path.class);
		when(workingDir.toAbsolutePath()).thenReturn(absoluteDir);

		// when
		Path dir = files.getProjectDir();

		// then
		assertThat(dir, equalTo(absoluteDir));
		verify(fileSystem, times(1)).getPath("");
		verify(workingDir, times(1)).toAbsolutePath();
	}

	@Test(expected = BugError.class)
	public void getProjectDir_GivenMissingProjectProperties_ThenBugError() throws IOException {
		// given
		Path workingDir = mock(Path.class);
		when(fileSystem.getPath("")).thenReturn(workingDir);

		Path absoluteDir = mock(Path.class);
		when(workingDir.toAbsolutePath()).thenReturn(absoluteDir);

		Path propertiesFile = mock(Path.class);
		when(absoluteDir.resolve(".project.properties")).thenReturn(propertiesFile);
		doThrow(IOException.class).when(provider).checkAccess(propertiesFile);

		// when
		files.getProjectDir();

		// then
	}

	@Test
	public void getFileName() {
		// given
		Path file = mock(Path.class);
		Path name = mock(Path.class);
		when(file.getFileName()).thenReturn(name);
		when(name.toString()).thenReturn("dialog.htm");

		// when
		String fileName = files.getFileName(file);

		// then
		assertThat(fileName, equalTo("dialog.htm"));
	}

	@Test
	public void getFileBasename() {
		// given
		Path file = mock(Path.class);
		Path name = mock(Path.class);
		when(file.getFileName()).thenReturn(name);
		when(name.toString()).thenReturn("dialog.htm");

		// when
		String basename = files.getFileBasename(file);

		// then
		assertThat(basename, equalTo("dialog"));
	}

	@Test
	public void getFileBasename_GivenMissingExtension_ThenReturnGivenName() {
		// given
		Path file = mock(Path.class);
		Path name = mock(Path.class);
		when(file.getFileName()).thenReturn(name);
		when(name.toString()).thenReturn("dialog");

		// when
		String basename = files.getFileBasename(file);

		// then
		assertThat(basename, equalTo("dialog"));
	}

	@Test
	public void GivenFileNameWithExtension_WhenGetExtension_ThenGotIt() {
		// given
		Path file = mock(Path.class);
		Path name = mock(Path.class);
		when(file.getFileName()).thenReturn(name);
		when(name.toString()).thenReturn("dialog.htm");

		// when
		String extension = files.getExtension(file);

		// then
		assertThat(extension, equalTo("htm"));
	}

	@Test
	public void GivenFileNameWithoutExtension_WhenGetExtension_ThenEmptyString() {
		// given
		Path file = mock(Path.class);
		Path name = mock(Path.class);
		when(file.getFileName()).thenReturn(name);
		when(name.toString()).thenReturn("dialog");

		// when
		String extension = files.getExtension(file);

		// then
		assertThat(extension, equalTo(""));
	}

    @Test
    public void GivenFilePathWithExtension_WhenChangeExtension_ThenPathWithNewExtension() {
      // given
      Path layoutFile = mock(Path.class);
      when(layoutFile.toString()).thenReturn("res/compo/dialog.htm");
      
      Path scriptFile = mock(Path.class); 
      when(fileSystem.getPath("res/compo/dialog.js")).thenReturn(scriptFile);
      when(scriptFile.toString()).thenReturn("res/compo/dialog.js");
      
      // when
      Path resultFile = files.changeExtension(layoutFile, "js");
      
      // then
      assertThat(resultFile.toString(), equalTo("res/compo/dialog.js"));
    }

    @Test
    public void GivenFilePathWithoutExtension_WhenChangeExtension_ThenPathWithNewExtension() {
      // given
      Path genericFile = mock(Path.class);
      when(genericFile.toString()).thenReturn("res/compo/dialog");
      
      Path scriptFile = mock(Path.class); 
      when(fileSystem.getPath("res/compo/dialog.js")).thenReturn(scriptFile);
      when(scriptFile.toString()).thenReturn("res/compo/dialog.js");
      
      // when
      Path resultFile = files.changeExtension(genericFile, "js");
      
      // then
      assertThat(resultFile.toString(), equalTo("res/compo/dialog.js"));
    }
	
    @Test
    public void GivenDirDoesNotExist_WhenCreateDirectory_ThenProviderCreateDirectory() throws IOException {
        // given
        Path dir = mock(Path.class);
        doThrow(IOException.class).when(provider).checkAccess(dir);

        // when
        files.createDirectory(dir);

        // then
        verify(provider, times(1)).checkAccess(dir);
        verify(provider, times(1)).createDirectory(dir);
    }

    @Test
    public void GivenDirDoesExist_WhenCreateDirectory_ThenNotProviderCreateDirectory() throws IOException {
        // given
        Path dir = mock(Path.class);

        // when
        files.createDirectory(dir);

        // then
        verify(provider, times(1)).checkAccess(dir);
        verify(provider, times(0)).createDirectory(dir);
    }

	@Test
	public void createDirectories() throws IOException {
		// given
		Path dir = mock(Path.class);
		doThrow(IOException.class).when(provider).checkAccess(dir); // dir does not exists
		Path parent = mock(Path.class);
		Path name = mock(Path.class);

		when(fileSystem.getPath("res", "page", "about")).thenReturn(dir);
		when(dir.getParent()).thenReturn(parent);
		when(parent.relativize(dir)).thenReturn(dir);
		when(parent.resolve(name)).thenReturn(dir);
		when(dir.iterator()).thenReturn(Arrays.asList(name).iterator());

		// when
		files.createDirectories("res", "page", "about");

		// then
		verify(provider, times(1)).createDirectory(dir);
	}

	@Test
	public void delete() throws IOException {
		// given
		Path path = mock(Path.class);

		// when
		files.delete(path);

		// then
		verify(provider, times(1)).delete(path);
	}

	@Test
	public void move() throws IOException {
		// given
		Path source = mock(Path.class);
		Path target = mock(Path.class);

		// when
		files.move(source, target);

		// then
		verify(provider, times(1)).move(source, target);
	}

	@Test
	public void isDirectory() throws IOException {
		// given
		Path path = mock(Path.class);
		BasicFileAttributes attributes = mock(BasicFileAttributes.class);
		when(provider.readAttributes(path, BasicFileAttributes.class)).thenReturn(attributes);
		when(attributes.isDirectory()).thenReturn(true);

		// when
		boolean isDirectory = files.isDirectory(path);

		// then
		assertThat(isDirectory, equalTo(true));
		verify(provider, times(1)).readAttributes(path, BasicFileAttributes.class);
		verify(attributes, times(1)).isDirectory();
	}

	@Test
	public void exists() throws IOException {
		// given
		Path path = mock(Path.class);

		// when
		files.exists(path);

		// then
		verify(provider, times(1)).checkAccess(path);
	}

	@Test
	public void getPath() throws IOException {
		// given
		Path path = mock(Path.class);
		when(fileSystem.getPath(anyString())).thenReturn(path);
		when(path.toString()).thenReturn("res/page/about");

		// when
		Path returnPath = files.getPath("res/page/about");

		// then
		assertThat(returnPath, notNullValue());
		assertThat(returnPath.toString(), equalTo("res/page/about"));
		verify(fileSystem, times(1)).getPath("res/page/about");
	}

	@Test
	public void getReader() throws IOException {
		// given
		Path file = mock(Path.class);
		InputStream inputStream = new ByteArrayInputStream("test".getBytes());
		when(provider.newInputStream(file)).thenReturn(inputStream);

		// when
		Reader reader = files.getReader(file);

		// then
		assertThat(reader, notNullValue());
		verify(provider, times(1)).newInputStream(file);

		BufferedReader bufferedReader = new BufferedReader(reader);
		assertThat(bufferedReader.readLine(), equalTo("test"));
	}

	@Test
	public void getInputStream() throws IOException {
		// given
		Path file = mock(Path.class);
		InputStream inputStream = new ByteArrayInputStream("test".getBytes());
		when(provider.newInputStream(file)).thenReturn(inputStream);

		// when
		InputStream returnStream = files.getInputStream(file);

		// then
		assertThat(returnStream, notNullValue());
		verify(provider, times(1)).newInputStream(file);

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(returnStream));
		assertThat(bufferedReader.readLine(), equalTo("test"));
	}

	@Test
	public void getOutputStream() throws IOException {
		// given
		Path file = mock(Path.class);
		OutputStream outputStream = new ByteArrayOutputStream();
		when(provider.newOutputStream(file)).thenReturn(outputStream);

		// when
		OutputStream returnStream = files.getOutputStream(file);

		// then
		assertThat(returnStream, notNullValue());
		verify(provider, times(1)).newOutputStream(file);
	}

	@Test
	public void listFiles() throws IOException {
		// given
		Path dir = mock(Path.class);
		@SuppressWarnings("unchecked")
		DirectoryStream.Filter<Path> filter = mock(DirectoryStream.Filter.class);
		when(provider.newDirectoryStream(dir, filter)).thenReturn(directoryStream());

		// when
		Iterable<Path> dirFiles = files.listFiles(dir, filter);

		// then
		assertThat(dirFiles, notNullValue());
		verify(provider, times(1)).newDirectoryStream(dir, filter);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void listFiles_GivenNoFilter_ThenGenerateAcceptAll() throws IOException {
		// given
		Path dir = mock(Path.class);
		when(provider.newDirectoryStream(eq(dir), any(DirectoryStream.Filter.class))).thenReturn(directoryStream());

		// when
		Iterable<Path> dirFiles = files.listFiles(dir);

		// then
		assertThat(dirFiles, notNullValue());

		ArgumentCaptor<DirectoryStream.Filter<Path>> filterArgument = ArgumentCaptor.forClass(DirectoryStream.Filter.class);
		verify(provider, times(1)).newDirectoryStream(eq(dir), filterArgument.capture());
		assertTrue(filterArgument.getValue().accept(null));
		assertTrue(filterArgument.getValue().accept(dir));
	}

	// --------------------------------------------------------------------------------------------

	private static DirectoryStream<Path> directoryStream(Path... files) {
		return new DirectoryStream<Path>() {
			@Override
			public Iterator<Path> iterator() {
				return Arrays.asList(files).iterator();
			}

			@Override
			public void close() throws IOException {
			}
		};
	}
}
