package com.jslib.commons.cli;

import static java.lang.String.format;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathExpressionException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.xml.sax.SAXException;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.util.Classes;

public class HttpRequest
{
  private HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
  private DocumentBuilder documentBuilder = Classes.loadService(DocumentBuilder.class);

  public Document loadHTML(URI remoteFile) throws MalformedURLException, IOException, SAXException
  {
    return documentBuilder.loadHTML(remoteFile.toURL());
  }

  public Path download(URI remoteFile, Path localFile) throws IOException
  {
    return download(remoteFile, localFile, null);
  }

  public Path download(URI remoteFile, Path localFile, IProgress<Integer> progress) throws IOException
  {
    try (CloseableHttpClient client = httpClientBuilder.build()) {
      HttpGet httpGet = new HttpGet(remoteFile);
      try (CloseableHttpResponse response = client.execute(httpGet)) {
        if(response.getStatusLine().getStatusCode() != 200) {
          throw new IOException(format("Fail to download %s.", remoteFile));
        }

        byte[] buffer = new byte[65535];
        try (BufferedInputStream inputStream = new BufferedInputStream(response.getEntity().getContent()); BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(localFile), buffer.length)) {
          int len;
          while((len = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, len);
            if(progress != null) {
              progress.onProgress(len);
            }
          }
        }
      }
    }

    return localFile;
  }

  private static final DateTimeFormatter modificationTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public Iterable<IFile> getApacheDirectoryIndex(URI remoteDir, Pattern fileNamePattern) throws IOException, URISyntaxException, SAXException, XPathExpressionException
  {
    Document indexPageDoc = loadHTML(remoteDir);

    List<IFile> files = new ArrayList<>();
    for(Element linkElement : indexPageDoc.findByXPath("//*[@href]")) {
      String fileName = linkElement.getAttr("href");
      Matcher matcher = fileNamePattern.matcher(fileName);
      if(matcher.find()) {
        Element linkParent = linkElement.getParent();
        Element dateElement = linkParent.getNextSibling();
        ZonedDateTime modificationTime = LocalDateTime.parse(dateElement.getText().trim(), modificationTimeFormatter).atZone(ZoneId.of("UTC"));

        Element sizeElement = dateElement.getNextSibling();
        long fileSize = fileSize(sizeElement.getText().trim());

        files.add(new File(remoteDir.resolve(fileName), fileName, modificationTime, fileSize));
      }
    }

    return files;
  }

  public IFile scanLatestFileVersion(URI remoteDir, Pattern filePattern) throws IOException, URISyntaxException, XPathExpressionException, SAXException
  {
    return scanLatestFileVersion(remoteDir, filePattern, null);
  }

  public IFile scanLatestFileVersion(URI remoteDir, Pattern filePattern, IProgress<IFile> progress) throws IOException, URISyntaxException, XPathExpressionException, SAXException
  {
    IFile mostRecentFile = null;
    for(IFile file : getApacheDirectoryIndex(remoteDir, filePattern)) {
      if(progress != null) {
        progress.onProgress(file);
      }
      if(mostRecentFile == null) {
        mostRecentFile = file;
        continue;
      }
      if(file.isAfter(mostRecentFile)) {
        mostRecentFile = file;
      }
    }
    return mostRecentFile;
  }

  private static final Pattern FILE_SIZE_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)?)(K|M|G|T)?$", Pattern.CASE_INSENSITIVE);

  private static long fileSize(String value)
  {
    Matcher matcher = FILE_SIZE_PATTERN.matcher(value);
    if(!matcher.find()) {
      return 0L;
    }
    Double fileSize = Double.parseDouble(matcher.group(1));
    if(matcher.group(2) != null) {
      switch(matcher.group(2).toUpperCase()) {
      case "K":
        fileSize *= 1024;
        break;
      case "M":
        fileSize *= 1048576;
        break;
      case "G":
        fileSize *= 1073741824;
        break;
      case "T":
        fileSize *= 1099511627776L;
        break;
      }
    }
    return fileSize.longValue();
  }

  private static class File implements IFile
  {
    private final URI uri;
    private final String name;
    private final ZonedDateTime modificationTime;
    private final long size;

    public File(URI uri, String name, ZonedDateTime modificationTime, long size)
    {
      this.uri = uri;
      this.name = name;
      this.modificationTime = modificationTime;
      this.size = size;
    }

    @Override
    public URI getURI()
    {
      return uri;
    }

    @Override
    public String getName()
    {
      return name;
    }

    @Override
    public LocalDateTime getModificationTime()
    {
      return modificationTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    @Override
    public long getSize()
    {
      return size;
    }

    @Override
    public boolean isAfter(IFile other)
    {
      return this.modificationTime.isAfter(((File)other).modificationTime);
    }
  }
}
