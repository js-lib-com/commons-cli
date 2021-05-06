package com.jslib.commons.cli;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FilesUtilTreeWalkerTest {
	@Mock
	private FileSystemProvider provider;
	@Mock
	private FileSystem fileSystem;
	@Mock
	private Console console;

	@Mock
	private BasicFileAttributes fileAttrs;
	@Mock
	private Path sourceFile;
	@Mock
	private BasicFileAttributes dirAttrs;
	@Mock
	private Path sourceDir;
	@Mock
	private Path targetFile;
	@Mock
	private Path targetDir;
	@Mock
	private SimpleFileVisitor<Path> visitor;
	
	private FilesUtil files;

	@SuppressWarnings("unchecked")
	@Before
	public void beforeTest() throws IOException {
		when(fileSystem.provider()).thenReturn(provider);

		when(fileAttrs.isDirectory()).thenReturn(false);
		when(dirAttrs.isDirectory()).thenReturn(true);

		when(sourceFile.getFileSystem()).thenReturn(fileSystem);
		when(provider.readAttributes(sourceFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)).thenReturn(fileAttrs);

		when(sourceDir.getFileSystem()).thenReturn(fileSystem);
		when(provider.readAttributes(sourceDir, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)).thenReturn(dirAttrs);
		when(provider.newDirectoryStream(eq(sourceDir), any(DirectoryStream.Filter.class))).thenReturn(directoryStream(sourceFile));
		
		files = new FilesUtil(fileSystem, console);
	}

	@Test
	public void walkFileTree() throws IOException {
		// given
		when(visitor.preVisitDirectory(sourceDir, dirAttrs)).thenReturn(FileVisitResult.CONTINUE);
		when(visitor.visitFile(sourceFile, fileAttrs)).thenReturn(FileVisitResult.CONTINUE);
		when(visitor.postVisitDirectory(sourceDir, null)).thenReturn(FileVisitResult.CONTINUE);

		// when
		files.walkFileTree(sourceDir, visitor);

		// then
		// verify(provider, times(1)).checkAccess(path);
	}

	@Test
	public void cleanDirectory() throws IOException {
		// given

		// when
		files.cleanDirectory(sourceDir, true);

		// then
		verify(provider, times(1)).delete(sourceFile);
		verify(provider, times(0)).delete(sourceDir);
		verify(console, times(1)).print(anyString(), eq(sourceFile));
	}

	@Test
	public void cleanDirectory_GivenNotVerbose_ThenNoConsolePrint() throws IOException {
		// given

		// when
		files.cleanDirectory(sourceDir, false);

		// then
		verify(console, times(0)).print(anyString(), eq(sourceFile));
	}

	@Test(expected = IOException.class)
	public void cleanDirectory_GivenDirAttributesException_ThenIOException() throws IOException {
		// given
		when(provider.readAttributes(sourceDir, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)).thenThrow(IOException.class);

		// when
		files.cleanDirectory(sourceDir, true);

		// then
	}

	@Test
	public void copyFiles() throws IOException {
		// given
		Path relativeFile = mock(Path.class);
		when(sourceDir.relativize(sourceFile)).thenReturn(relativeFile);
		when(targetDir.resolve(relativeFile)).thenReturn(targetFile);
		
		Path parentDir = mock(Path.class);
		when(targetFile.getParent()).thenReturn(parentDir);

		// when
		files.copyFiles(sourceDir, targetDir, true);

		// then
		verify(sourceDir, times(1)).relativize(sourceFile);
		verify(targetDir, times(1)).resolve(relativeFile);
		verify(provider, times(1)).copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
		verify(console, times(1)).print(anyString(), eq(relativeFile));
	}

	@Test
	public void copyFiles_GivenNotVebose_ThenNoConsolePrint() throws IOException {
		// given
		Path relativeFile = mock(Path.class);
		when(sourceDir.relativize(sourceFile)).thenReturn(relativeFile);
		when(targetDir.resolve(relativeFile)).thenReturn(targetFile);
		
		Path parentDir = mock(Path.class);
		when(targetFile.getParent()).thenReturn(parentDir);
		
		// when
		files.copyFiles(sourceDir, targetDir, false);

		// then
		verify(console, times(0)).print(anyString(), eq(sourceFile));
	}

	@Test
	public void getFileByExtension() throws IOException {
		// given
		when(sourceFile.toString()).thenReturn("dialog.htm");

		// when
		Path file = files.getFileByExtension(sourceDir, ".htm");

		// then
		assertThat(file, notNullValue());
		assertThat(file, equalTo(sourceFile));
	}

	@Test
	public void getFileByExtension_GivenExtensionNotMatch_ThenNull() throws IOException {
		// given
		when(sourceFile.toString()).thenReturn("dialog.css");

		// when
		Path file = files.getFileByExtension(sourceDir, ".htm");

		// then
		assertThat(file, nullValue());
	}

	@Test
	public void findFilesByExtension() throws IOException {
		// given
		when(sourceFile.toString()).thenReturn("dialog.htm");

		// when
		List<Path> filesList = files.findFilesByExtension(sourceDir, ".htm");

		// then
		assertThat(filesList, notNullValue());
		assertThat(filesList, hasSize(1));
		assertThat(filesList, contains(sourceFile));
	}

	@Test
	public void findFilesByExtension_GivenExtensionNotMatch_ThenEmptyList() throws IOException {
		// given
		when(sourceFile.toString()).thenReturn("dialog.css");

		// when
		List<Path> filesList = files.findFilesByExtension(sourceDir, ".htm");

		// then
		assertThat(filesList, notNullValue());
		assertThat(filesList, hasSize(0));
	}

	@Test
	public void findFilesByContentPattern() throws IOException {
		// given
		when(sourceFile.toString()).thenReturn("dialog.htm");
		InputStream inputStream = new ByteArrayInputStream("<div w:compo='res/compo/dialog'></div>".getBytes());
		when(provider.newInputStream(sourceFile)).thenReturn(inputStream);

		// when
		List<Path> filesList = files.findFilesByContentPattern(sourceDir, ".htm", "res/compo/dialog");

		// then
		assertThat(filesList, notNullValue());
		assertThat(filesList, hasSize(1));
		assertThat(filesList, contains(sourceFile));
	}

	@Test
	public void findFilesByContentPattern_GivenExtensionNotMatch_ThenEmptyList() throws IOException {
		// given
		when(sourceFile.toString()).thenReturn("dialog.css");

		// when
		List<Path> filesList = files.findFilesByContentPattern(sourceDir, ".htm", "res/compo/dialog");

		// then
		assertThat(filesList, notNullValue());
		assertThat(filesList, hasSize(0));
	}

	@Test
	public void findFilesByContentPattern_GivenContentNotMatch_ThenEmptyList() throws IOException {
		// given
		when(sourceFile.toString()).thenReturn("dialog.htm");
		// res/compo/dialoc is spelled incorrectly
		InputStream inputStream = new ByteArrayInputStream("<div w:compo='res/compo/dialoc'></div>".getBytes());
		when(provider.newInputStream(sourceFile)).thenReturn(inputStream);

		// when
		List<Path> filesList = files.findFilesByContentPattern(sourceDir, ".htm", "res/compo/dialog");

		// then
		assertThat(filesList, notNullValue());
		assertThat(filesList, hasSize(0));
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
