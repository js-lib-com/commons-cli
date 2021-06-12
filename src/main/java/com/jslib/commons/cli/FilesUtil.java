package com.jslib.commons.cli;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import js.lang.BugError;
import js.util.Params;
import js.util.Strings;

public class FilesUtil
{
  private final FileSystem fileSystem;
  private final IConsole console;

  public FilesUtil()
  {
    this.fileSystem = null;
    this.console = null;
  }

  public FilesUtil(FileSystem fileSystem, IConsole console)
  {
    this.fileSystem = fileSystem;
    this.console = console;
  }

  public Path getWorkingDir()
  {
    return fileSystem.getPath("").toAbsolutePath();
  }

  public Path getProjectDir()
  {
    Path projectDir = fileSystem.getPath("").toAbsolutePath();
    Path propertiesFile = projectDir.resolve(".project.properties");
    if(!exists(propertiesFile)) {
      throw new BugError("Invalid project. Missing project properties file %s.", propertiesFile);
    }
    return projectDir;
  }

  public String getFileName(Path file)
  {
    return file.getFileName().toString();
  }

  public List<String> getFileNames(Path dir) throws IOException
  {
    List<String> fileNames = new ArrayList<>();
    for(Path file : listFiles(dir)) {
      fileNames.add(getFileBasename(file));
    }
    return fileNames;
  }

  public String getFileBasename(Path file)
  {
    String fileName = file.getFileName().toString();
    int i = fileName.lastIndexOf('.');
    return i != -1 ? fileName.substring(0, i) : fileName;
  }

  public String getExtension(Path file)
  {
    String path = file.getFileName().toString();
    int extensionPos = path.lastIndexOf('.');
    return extensionPos == -1 ? "" : path.substring(extensionPos + 1).toLowerCase();
  }

  public boolean hasExtension(Path file, String extension)
  {
    return file.toString().endsWith(extension);
  }

  public Path changeExtension(Path file, String extension)
  {
    String path = file.toString();
    int extensionPos = path.lastIndexOf('.');
    if(extensionPos == -1) {
      return fileSystem.getPath(Strings.concat(path, '.', extension));
    }
    return fileSystem.getPath(path.substring(0, extensionPos + 1) + extension);
  }

  public LocalDateTime getModificationTime(Path file) throws IOException
  {
    FileTime fileTime = fileSystem.provider().readAttributes(file, BasicFileAttributes.class).lastModifiedTime();
    return LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
  }

  public void createDirectory(Path dir) throws IOException
  {
    Params.notNull(dir, "Directory");
    if(!exists(dir)) {
      fileSystem.provider().createDirectory(dir);
    }
  }

  public Path createDirectories(String first, String... more) throws IOException
  {
    Params.notNullOrEmpty(first, "First path component");
    Params.notNull(more, "More path components");
    return createDirectories(fileSystem.getPath(first, more));
  }

  public Path createDirectories(Path dir) throws IOException
  {
    if(exists(dir)) {
      return dir;
    }

    Path parent = dir.getParent();
    while(parent != null) {
      if(exists(parent)) {
        break;
      }
      parent = parent.getParent();
    }
    if(parent == null) {
      throw new FileSystemException(dir.toString(), null, "Unable to determine if root directory exists");
    }

    Path child = parent;
    for(Path name : parent.relativize(dir)) {
      child = child.resolve(name);
      createDirectory(child);
    }

    return dir;
  }

  public void cleanDirectory(Path rootDir, boolean verbose, Path... excludes) throws IOException
  {
    List<Path> excludesList = Arrays.asList(excludes);
    // walk file tree is depth-first so that the most inner files and directories are removed first
    walkFileTree(rootDir, new SimpleFileVisitor<Path>()
    {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
      {
        if(excludesList.contains(file)) {
          return FileVisitResult.CONTINUE;
        }
        if(verbose) {
          console.print("Delete file %s.", file);
        }
        delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
      {
        if(exc != null) {
          throw exc;
        }
        if(rootDir.equals(dir)) {
          return FileVisitResult.CONTINUE;
        }
        if(verbose) {
          console.print("Delete directory %s.", dir);
        }
        delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  public void delete(Path path) throws IOException
  {
    fileSystem.provider().delete(path);
  }

  public void deleteIfExists(Path path) throws IOException
  {
    if(path != null && exists(path)) {
      fileSystem.provider().delete(path);
    }
  }

  public void move(Path source, Path target) throws IOException
  {
    fileSystem.provider().move(source, target);
  }

  public boolean isDirectory(Path path)
  {
    try {
      return fileSystem.provider().readAttributes(path, BasicFileAttributes.class).isDirectory();
    }
    catch(IOException unused) {
      return false;
    }
  }

  public boolean isEmpty(Path dir) throws IOException
  {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      return !stream.iterator().hasNext();
    }
  }

  public boolean exists(Path file)
  {
    try {
      fileSystem.provider().checkAccess(file);
      return true;
    }
    catch(IOException unused) {
      return false;
    }
  }

  public Path getPath(String path)
  {
    return fileSystem.getPath(path);
  }

  public Reader getReader(Path file) throws IOException
  {
    return new InputStreamReader(fileSystem.provider().newInputStream(file), "UTF-8");
  }

  public Writer getWriter(Path file) throws IOException
  {
    createDirectory(file.getParent());
    return new OutputStreamWriter(fileSystem.provider().newOutputStream(file), "UTF-8");
  }

  public InputStream getInputStream(Path file) throws IOException
  {
    return fileSystem.provider().newInputStream(file);
  }

  public OutputStream getOutputStream(Path file) throws IOException
  {
    return fileSystem.provider().newOutputStream(file);
  }

  public Iterable<Path> listFiles(Path dir, DirectoryStream.Filter<Path> filter) throws IOException
  {
    return fileSystem.provider().newDirectoryStream(dir, filter);
  }

  public Iterable<Path> listFiles(Path dir) throws IOException
  {
    return listFiles(dir, path -> true);
  }

  public void walkFileTree(Path start, FileVisitor<Path> visitor) throws IOException
  {
    Files.walkFileTree(start, visitor);
  }

  public void copy(Path sourceFile, Path targetFile) throws IOException
  {
    createDirectories(targetFile.getParent());
    copy(getReader(sourceFile), getWriter(targetFile));
  }

  public void copy(String source, Path targetFile) throws IOException
  {
    createDirectories(targetFile.getParent());
    copy(new StringReader(source), getWriter(targetFile));
  }

  public void copy(Reader reader, Writer writer) throws IOException
  {
    try (BufferedReader bufferedReader = new BufferedReader(reader); BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
      char[] buffer = new char[1024];
      int length;
      while((length = bufferedReader.read(buffer, 0, buffer.length)) != -1) {
        bufferedWriter.write(buffer, 0, length);
      }
    }
  }

  public void copy(InputStream inputStream, Path targetFile) throws IOException
  {
    createDirectories(targetFile.getParent());
    try (OutputStream outputStream = getOutputStream(targetFile)) {
      byte[] buffer = new byte[1024];
      int length;
      while((length = inputStream.read(buffer, 0, buffer.length)) != -1) {
        outputStream.write(buffer, 0, length);
      }
    }
  }

  public void copyFiles(Path sourceDir, Path targetDir, boolean verbose) throws IOException
  {
    walkFileTree(sourceDir, new SimpleFileVisitor<Path>()
    {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
      {
        Path relativeFile = sourceDir.relativize(file);
        if(verbose) {
          console.print("Copy file %s", relativeFile);
        }
        Path targetFile = targetDir.resolve(relativeFile);
        createDirectory(targetFile.getParent());
        fileSystem.provider().copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  public Path getFileByExtension(Path dir, String extension) throws IOException
  {
    class FoundFile
    {
      Path path = null;
    }
    final FoundFile foundFile = new FoundFile();

    walkFileTree(dir, new SimpleFileVisitor<Path>()
    {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
      {
        if(hasExtension(file, extension)) {
          foundFile.path = file;
          return FileVisitResult.TERMINATE;
        }
        return FileVisitResult.CONTINUE;
      }
    });
    return foundFile.path;
  }

  public Path getFileByNamePattern(Path dir, Pattern pattern) throws IOException
  {
    class FoundFile
    {
      Path path = null;
    }
    final FoundFile foundFile = new FoundFile();

    walkFileTree(dir, new SimpleFileVisitor<Path>()
    {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
      {
        String fileName = file.getFileName().toString();
        Matcher matcher = pattern.matcher(fileName);
        if(matcher.find()) {
          foundFile.path = file;
          return FileVisitResult.TERMINATE;
        }
        return FileVisitResult.CONTINUE;
      }
    });
    return foundFile.path;
  }

  public List<Path> findFilesByExtension(Path dir, String extension) throws IOException
  {
    List<Path> files = new ArrayList<>();
    walkFileTree(dir, new SimpleFileVisitor<Path>()
    {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
      {
        if(hasExtension(file, extension)) {
          files.add(file);
        }
        return FileVisitResult.CONTINUE;
      }
    });
    return files;
  }

  public List<Path> findFilesByContentPattern(Path dir, String extension, String pattern) throws IOException
  {
    List<Path> files = new ArrayList<>();
    walkFileTree(dir, new SimpleFileVisitor<Path>()
    {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
      {
        if(hasExtension(file, extension)) {
          if(Strings.load(getReader(file)).contains(pattern)) {
            files.add(file);
          }
        }
        return FileVisitResult.CONTINUE;
      }
    });
    return files;
  }

  public void setLastModifiedTime(Path file, FileTime time) throws IOException
  {
    Files.setLastModifiedTime(file, time);
  }

  public boolean isXML(Path file, String... roots) throws IOException
  {
    try (BufferedReader reader = new BufferedReader(getReader(file))) {
      String line = reader.readLine();
      if(line.startsWith("<?")) {
        line = reader.readLine();
      }
      for(String root : roots) {
        if(line.startsWith(Strings.concat('<', root, '>'))) {
          return true;
        }
      }
    }
    return false;
  }
}
