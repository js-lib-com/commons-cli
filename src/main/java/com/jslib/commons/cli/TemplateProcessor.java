package com.jslib.commons.cli;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import js.io.VariablesWriter;
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
    try (ZipInputStream zipInputStream = template(type, templateName)) {
      ZipEntry zipEntry;
      while((zipEntry = zipInputStream.getNextEntry()) != null) {
        String zipEntryName = Strings.injectVariables(zipEntry.getName(), variables);

        if(zipEntryName.endsWith("/")) {
          mkdirs(zipEntryName);
          continue;
        }

        String[] zipEntryNameSegments = zipEntryName.split("/");
        String fileName = zipEntryNameSegments[zipEntryNameSegments.length - 1];
        // by convention, for formatted files, file name starts with dollar ($)
        if(fileName.startsWith("!")) {
          zipEntryNameSegments[zipEntryNameSegments.length - 1] = fileName.substring(1);
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
    char[] buffer = new char[2048];

    File file = new File(targetDir, zipEntryName);
    if(verbose) {
      print("Create file '%s'.", file);
    }

    Reader reader = new InputStreamReader(zipInputStream);
    try (Writer writer = new VariablesWriter(new BufferedWriter(new FileWriter(file)), variables)) {
      int len;
      while((len = reader.read(buffer)) > 0) {
        writer.write(buffer, 0, len);
      }
    }
  }

  private void copy(ZipInputStream zipInputStream, String zipEntryName) throws IOException
  {
    byte[] buffer = new byte[2048];

    File file = new File(targetDir, zipEntryName);
    if(verbose) {
      print("Create file '%s'.", file);
    }

    try (BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(file), buffer.length)) {
      int len;
      while((len = zipInputStream.read(buffer)) > 0) {
        fileOutputStream.write(buffer, 0, len);
      }
    }
  }

  private static ZipInputStream template(String type, String name) throws FileNotFoundException
  {
    File woodHomeDir = new File(Home.getPath());
    File templateFile = new File(woodHomeDir, Strings.concat("template", File.separatorChar, type, File.separatorChar, name, ".zip"));
    return new ZipInputStream(new BufferedInputStream(new FileInputStream(templateFile)));
  }

  protected static void print(String format, Object... args)
  {
    System.out.printf(format, args);
    System.out.println();
  }
}
