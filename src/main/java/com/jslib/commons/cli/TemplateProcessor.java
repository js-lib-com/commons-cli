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
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.velocity.VelocityContext;

import com.jslib.util.Strings;

public class TemplateProcessor
{
  private static final String TEMPLATE_EXT = ".vtl";

  private final Map<String, String> excludedFiles = new HashMap<>();

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

  public Reader getExcludedFileReader(String fileName)
  {
    String content = excludedFiles.get(fileName);
    return content != null ? new StringReader(content) : null;
  }

  public void exec(String type, String templateName, Map<String, String> variables) throws IOException
  {
    File woodHomeDir = new File(Home.getPath());
    File templateFile = new File(woodHomeDir, Strings.concat("template", File.separatorChar, type, File.separatorChar, templateName, ".zip"));
    exec(templateFile, variables);
  }

  public void exec(File templateFile, Map<String, String> variables) throws IOException
  {
    try (ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(templateFile)))) {
      ZipEntry zipEntry;
      while((zipEntry = zipInputStream.getNextEntry()) != null) {
        Flag flag = new Flag(zipEntry.getName(), variables);
        String entryName = Strings.injectVariables(flag.entryName, variables);
        boolean directory = entryName.endsWith("/");

        String[] zipEntryNameSegments = entryName.split("/");
        String fileName = zipEntryNameSegments[zipEntryNameSegments.length - 1];
        boolean template = false;
        if(fileName.endsWith(TEMPLATE_EXT)) {
          template = true;
          fileName = fileName.substring(0, fileName.length() - TEMPLATE_EXT.length());
          zipEntryNameSegments[zipEntryNameSegments.length - 1] = fileName;
        }

        // ${page}~{compo-script}.js.vtl
        // if there is no 'compo-script' variable equal with 'true' then ignores zip entry
        if(flag.value != null && !"true".equalsIgnoreCase(variables.get(flag.value))) {
          if(!directory) {
            StringWriter writer = new StringWriter();
            if(template) {
              copy(zipInputStream, Strings.join(zipEntryNameSegments, '/'), writer, variables);
            }
            else {
              copy(zipInputStream, writer);
            }
            excludedFiles.put(fileName, writer.toString());
          }
          continue;
        }

        if(directory) {
          mkdirs(entryName);
          continue;
        }

        // by convention, for formatted files, file name has .vtl extension
        if(template) {
          copy(zipInputStream, Strings.join(zipEntryNameSegments, '/'), variables);
        }
        else {
          copy(zipInputStream, entryName);
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
    copy(zipInputStream, zipEntryName, new FileWriter(file), variables);
  }

  private void copy(ZipInputStream zipInputStream, String zipEntryName, Writer writer, Map<String, String> variables) throws IOException
  {
    VelocityContext context = new VelocityContext();
    for(Map.Entry<String, String> entry : variables.entrySet()) {
      context.put(entry.getKey(), entry.getValue());
    }

    Reader reader = new UncloseableReader(new InputStreamReader(zipInputStream));
    try (Writer bufferedWriter = new BufferedWriter(writer)) {
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

  private void copy(ZipInputStream zipInputStream, Writer writer) throws IOException
  {
    char[] buffer = new char[2048];
    int len;
    try (Reader reader = new UncloseableReader(new InputStreamReader(zipInputStream)); Writer bufferedWriter = new BufferedWriter(writer)) {
      while((len = reader.read(buffer)) > 0) {
        writer.write(buffer, 0, len);
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

  private static class Flag
  {
    final String entryName;
    final String value;

    Flag(String entryName, Map<String, String> variables) throws IOException
    {
      int start = entryName.indexOf("~{");
      if(start == -1) {
        this.entryName = entryName;
        this.value = null;
        return;
      }
      int end = entryName.indexOf('}', start);
      if(end == -1) {
        throw new IOException(Strings.format("Invalid ZIP entery name %s. Missing flag end mark.", entryName));
      }

      this.value = entryName.substring(start + 2, end);

      StringBuilder stringBuilder = new StringBuilder();
      for(int i = 0; i < start; ++i) {
        stringBuilder.append(entryName.charAt(i));
      }
      for(int i = end + 1; i < entryName.length(); ++i) {
        stringBuilder.append(entryName.charAt(i));
      }
      this.entryName = stringBuilder.toString();
    }

    @Override
    public String toString()
    {
      return "Flag [entryName=" + entryName + ", value=" + value + "]";
    }
  }
}
