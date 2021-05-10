package com.jslib.commons.cli;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.velocity.VelocityContext;

import js.util.Strings;

public class TemplateProcessor
{
  private File targetDir;
  private boolean verbose;

  public void setTargetDir(File targetDir)
  {
    this.targetDir = targetDir;
  }

  public void setVerbose(boolean verbose)
  {
    this.verbose = verbose;
  }

  public void exec(String type, String templateName, Map<String, String> variables) throws IOException
  {
    File woodHomeDir = new File(Home.getPath());
    File templateFile = new File(woodHomeDir, Strings.concat("template", File.separatorChar, type, File.separatorChar, templateName, ".zip"));

    try (ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(templateFile)))) {
      ZipEntry zipEntry;
      while((zipEntry = zipInputStream.getNextEntry()) != null) {
        String zipEntryName = Strings.injectVariables(zipEntry.getName(), variables);

        if(zipEntryName.endsWith("/")) {
          mkdirs(zipEntryName);
          continue;
        }

        String[] zipEntryNameSegments = zipEntryName.split("/");
        String fileName = zipEntryNameSegments[zipEntryNameSegments.length - 1];
        // by convention, for formatted files, file name has .vtl extension
        if(fileName.endsWith(".vtl")) {
          zipEntryNameSegments[zipEntryNameSegments.length - 1] = fileName.substring(0, fileName.length() - 4);
          copy(zipInputStream, Strings.join(zipEntryNameSegments, '/'), variables);
        }
        else {
          copy(zipInputStream, zipEntryName);
        }
      }
    }
  }

  private void mkdirs(String path) throws IOException
  {
    File dir = new File(targetDir, path);
    if(verbose) {
      print("Create directory '%s'.", dir);
    }
    if(!dir.mkdirs()) {
      throw new IOException("Cannot create directory " + dir);
    }
  }

  private void copy(ZipInputStream zipInputStream, String zipEntryName, Map<String, String> variables) throws IOException
  {
    File file = new File(targetDir, zipEntryName);
    if(verbose) {
      print("Create file '%s'.", file);
    }

    VelocityContext context = new VelocityContext();
    for(Map.Entry<String, String> entry : variables.entrySet()) {
      context.put(entry.getKey(), entry.getValue());
    }

    Reader reader = new UncloseableReader(new InputStreamReader(zipInputStream));
    try (Writer writer = new BufferedWriter(new FileWriter(file))) {
      org.apache.velocity.app.Velocity.evaluate(context, writer, zipEntryName, reader);
    }
  }

  private void copy(ZipInputStream zipInputStream, String zipEntryName) throws IOException
  {
    File file = new File(targetDir, zipEntryName);
    if(verbose) {
      print("Create file '%s'.", file);
    }

    byte[] buffer = new byte[2048];
    try (BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(file), buffer.length)) {
      int len;
      while((len = zipInputStream.read(buffer)) > 0) {
        fileOutputStream.write(buffer, 0, len);
      }
    }
  }

  protected static void print(String format, Object... args)
  {
    System.out.printf(format, args);
    System.out.println();
  }

  private static class UncloseableReader extends Reader
  {
    private final Reader reader;

    public UncloseableReader(Reader reader)
    {
      super();
      this.reader = reader;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException
    {
      return reader.read(cbuf, off, len);
    }

    @Override
    public void close() throws IOException
    {
    }
  }
}
