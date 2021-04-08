package models;

public class ObjectModel {
  private String bucketName;
  private String filePath;
  private String uri;
  private String contentType;

  public ObjectModel(String bucketName, String filePath, String uri, String contentType) {
    this.filePath = filePath;
    this.uri = uri;
    this.contentType = contentType;
    this.bucketName = bucketName;
  }

  public String getBucketName() {
    return bucketName;
  }

  public String getFilePath() {
    return filePath;
  }

  public String getUri() {
    return uri;
  }

  public String getContentType() {
    return contentType;
  }
}
