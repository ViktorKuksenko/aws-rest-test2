package services;

import helpers.FileUtils;
import helpers.PropertiesUtils;
import helpers.Sha256Utils;
import helpers.TimeUtils;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class AwsService {

  private String awsAccessKeyId;
  private String awsSecretAccessKeyId;
  private String regionName;
  private String httpMethod;
  private String payload;
  private String canonicalUri;
  private String bucketName;
  private String pathToFile;
  private List<String> canonicalHeaders;
  private List<String> signedHeaders;
  private String timestamp;
  private String date;
  private String acl;
  private String contentType;
  private boolean hashPayloadAsAString;
  private static final String REQUEST_ALGORITHM = "AWS4-HMAC-SHA256";
  private static final String SERVICE_NAME = "s3";
  private static final String X_AMZ_CONTENT_SHA_256_HEADER = "x-amz-content-sha256";
  private static final String X_AMZ_DATE_HEADER = "x-amz-date";
  private static final String HOST_HEADER = "host";
  private static final String X_AMZ_ACL_HEADER = "x-amz-acl";

  public AwsService() {
    PropertiesUtils propertiesUtils = new PropertiesUtils();
    Properties credentialProperties = propertiesUtils.getProperties(
        "C:\\Users\\vkukse\\Desktop\\EducationProjects\\rest-onedrive\\src\\files\\"
            + "credentials.properties");
    awsAccessKeyId = credentialProperties.getProperty("aws.accessKeyId");
    awsSecretAccessKeyId = credentialProperties.getProperty("aws.secretAccessKeyId");
    Properties configProperties = propertiesUtils.getProperties(
        "C:\\Users\\vkukse\\Desktop\\EducationProjects\\rest-onedrive\\src\\files\\"
            + "config.properties");
    regionName = configProperties.getProperty("aws.regionName");

    canonicalHeaders = Arrays.asList(X_AMZ_CONTENT_SHA_256_HEADER, X_AMZ_DATE_HEADER);
    signedHeaders = Arrays.asList(HOST_HEADER, X_AMZ_CONTENT_SHA_256_HEADER, X_AMZ_DATE_HEADER);
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    timestamp = TimeUtils.getTimeStamp(now);
    date = TimeUtils.getDate(now);
  }

  public static AwsService getInstance() {
    return new AwsService();
  }

  public AwsService setPayload(String payload) {
    this.payload = payload;
    return this;
  }

  public AwsService setCanonicalUri(String canonicalUri) {
    this.canonicalUri = canonicalUri;
    return this;
  }

  public AwsService setBucketName(String bucketName) {
    this.bucketName = bucketName;
    return this;
  }

  public AwsService setPathToFile(String pathToFile) {
    this.pathToFile = pathToFile;
    return this;
  }

  public AwsService setAcl(String acl) {
    this.acl = acl;
    return this;
  }

  public AwsService setContentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  public AwsService setHashPayloadAsAString(boolean hashPayloadAsAString) {
    this.hashPayloadAsAString = hashPayloadAsAString;
    return this;
  }

  private String getHashedPayLoad() {
    String hashedPayLoad = null;
    if (hashPayloadAsAString) {
      hashedPayLoad = Sha256Utils.getHexString(Sha256Utils.generateSHA256Hash(payload
          , "SHA-256"));
    } else {
      hashedPayLoad = FileUtils.getFileChecksum(new File(payload));
    }
    return hashedPayLoad;
  }

  private String getEncryptedCanonicalRequest(String canonicalRequest) {
    return Sha256Utils.getHexString(Sha256Utils.generateSHA256Hash(canonicalRequest
        , "SHA-256"));
  }

  private byte[] getEncryptedSignInKey(String awsSecretAccessKey, String dateRegionKey
      , String region, String service) {
    byte[] signInKey = null;
    try {
      signInKey = Sha256Utils.getSigInKey(awsSecretAccessKey
          , dateRegionKey, region, service);
    } catch (NoSuchAlgorithmException | InvalidKeyException
        | UnsupportedEncodingException exception) {
      exception.printStackTrace();
      exception.getCause();
    }
    return signInKey;
  }

  private String getAuthenticationSignature(String stringToSign, byte[] signInKey) {
    String signature = "";
    try {
      signature += Sha256Utils
          .getHexString(Sha256Utils.getHmacSHA256(stringToSign, signInKey));
    } catch (NoSuchAlgorithmException | InvalidKeyException
        | UnsupportedEncodingException exception) {
      exception.printStackTrace();
      exception.getCause();
    }
    return signature.trim();
  }

  private String getSignedHeaders() {
    StringBuilder stringBuilder = new StringBuilder("");
    if (!signedHeaders.isEmpty()) {
      for (int i = 0; i < signedHeaders.size(); i++) {
        if (i < signedHeaders.size() - 1) {
          stringBuilder.append(signedHeaders.get(i)).append(";");
        } else {
          stringBuilder.append(signedHeaders.get(i));
        }
      }
    } else {
      throw new RuntimeException("Signed headers are empty!");
    }
    return stringBuilder.toString();
  }

  private String getBaseUri() {
    return String.format("%s.%s.%s.amazonaws.com", bucketName, SERVICE_NAME, regionName);
  }

  private String getCanonicalRequest() {
    StringBuilder canonicalRequest = new StringBuilder("");

    canonicalRequest.append(httpMethod)
        .append("\n");

    if (canonicalUri.equals("") || canonicalUri.trim().equals("")) {
      canonicalRequest.append("/")
          .append("\n\n");
    } else if (canonicalUri.equals("acl=")) {
      canonicalRequest.append(getCanonicalUriForAclRequest())
          .append("\n");
    } else {
      canonicalRequest.append(canonicalUri)
          .append("\n\n");
    }
    canonicalRequest.append("host:" + getBaseUri())
        .append("\n");

    if (!canonicalHeaders.isEmpty()) {
      if (canonicalHeaders.size() == 3) {
        canonicalRequest.append(getCanonicalHeadersForBucketCreationRequest());
      } else {
        canonicalRequest.append(String.format("%s:%s", canonicalHeaders.get(0), getHashedPayLoad()))
            .append("\n");
        canonicalRequest.append(String.format("%s:%s", canonicalHeaders.get(1), timestamp))
            .append("\n");
      }
    } else {
      throw new RuntimeException("Canonical headers are empty!");
    }

    canonicalRequest.append("\n")
        .append(getSignedHeaders())
        .append("\n")
        .append(getHashedPayLoad());

    return canonicalRequest.toString();
  }

  private String getCanonicalUriForAclRequest() {
    StringBuilder stringBuilder = new StringBuilder("");
    stringBuilder.append("/")
        .append("\n")
        .append(canonicalUri);
    return stringBuilder.toString();
  }

  private String getCanonicalHeadersForBucketCreationRequest() {
    StringBuilder stringBuilder = new StringBuilder("");
    stringBuilder.append(String.format("%s:%s", canonicalHeaders.get(0), acl))
        .append("\n")
        .append(String.format("%s:%s", canonicalHeaders.get(1), getHashedPayLoad()))
        .append("\n")
        .append(String.format("%s:%s", canonicalHeaders.get(2), timestamp))
        .append("\n");
    return stringBuilder.toString();
  }

  private String getStringToSignIn() {
    String stringToSignIn = String.format("%s\n%s\n%s/%s/%s/aws4_request\n%s", REQUEST_ALGORITHM
        , timestamp, date, regionName, SERVICE_NAME
        , getEncryptedCanonicalRequest(getCanonicalRequest()));
    return stringToSignIn;
  }

  private String getSignature() {
    byte[] signInKey = getEncryptedSignInKey(awsSecretAccessKeyId, date, regionName, SERVICE_NAME);
    return getAuthenticationSignature(getStringToSignIn(), signInKey);
  }

  private String buildRequestData() {
    String credentialScope = String.format("%s/%s/%s/aws4_request", date, regionName, SERVICE_NAME);
    String authorizationString = String.format("%s Credential=%s/%s, SignedHeaders=%s, Signature="
            + "%s", REQUEST_ALGORITHM, awsAccessKeyId, credentialScope, getSignedHeaders()
        , getSignature());
    return authorizationString;
  }

  private RequestSpecification getCommonRequestSpecification(boolean addCanonicalUri) {
    String baseUri = "";
    if (addCanonicalUri) {
      baseUri = "https://" + getBaseUri() + canonicalUri;
    } else {
      baseUri = "https://" + getBaseUri();
    }
    Map<String, String> headerMap = new HashMap<>();
    headerMap.put("X-Amz-Content-Sha256", getHashedPayLoad());
    headerMap.put("X-Amz-Date", timestamp);
    headerMap.put("Authorization", buildRequestData());
    headerMap.put("Host", getBaseUri());
    RequestSpecification requestSpecification = new RequestSpecBuilder()
        .setBaseUri(baseUri)
        .addHeaders(headerMap)
        .build();

    return requestSpecification;
  }

  public Response getBucketContents() {
    httpMethod = "GET";
    Response response = RestAssured.given()
        .spec(getCommonRequestSpecification(false))
        .get()
        .then()
        .extract()
        .response();
    response.then()
        .log()
        .all();
    return response;
  }

  public Response getBucketAcl() {
    httpMethod = "GET";
    Response response = RestAssured.given()
        .spec(getCommonRequestSpecification(false))
        .queryParam("acl")
        .get()
        .then()
        .extract()
        .response();
    response.then()
        .log()
        .all();
    return response;
  }

  public Response createBucket() {
    httpMethod = "PUT";
    canonicalHeaders = Arrays
        .asList(X_AMZ_ACL_HEADER, X_AMZ_CONTENT_SHA_256_HEADER, X_AMZ_DATE_HEADER);
    signedHeaders = Arrays
        .asList(HOST_HEADER, X_AMZ_ACL_HEADER, X_AMZ_CONTENT_SHA_256_HEADER, X_AMZ_DATE_HEADER);
    RestAssured.baseURI = "https://" + getBaseUri();
    RestAssured.urlEncodingEnabled = false;
    Map<String, Object> headerMap = new HashMap<>();
    headerMap.put("X-Amz-Acl", acl);
    headerMap.put("X-Amz-Content-Sha256", getHashedPayLoad());
    headerMap.put("X-Amz-Date", timestamp);
    headerMap.put("Authorization", buildRequestData());
    headerMap.put("Host", getBaseUri());
    Response response = RestAssured.given()
        .headers(headerMap)
        .body(new File(pathToFile))
        .put()
        .then()
        .extract()
        .response();
    response.then()
        .log()
        .all();
    return response;
  }

  public Response deleteBucket() {
    httpMethod = "DELETE";
    Response response = RestAssured.given()
        .spec(getCommonRequestSpecification(false))
        .delete()
        .then()
        .extract()
        .response();
    response.then()
        .log()
        .all();
    return response;
  }

  public Response createObject() {
    httpMethod = "PUT";
    Response response = RestAssured.given()
        .spec(getCommonRequestSpecification(true))
        .body(new File(pathToFile))
        .put()
        .then()
        .extract()
        .response();
    response.then()
        .log()
        .all();
    return response;
  }

  public Response deleteObject() {
    httpMethod = "DELETE";
    Response response = RestAssured.given()
        .spec(getCommonRequestSpecification(true))
        .delete()
        .then()
        .extract()
        .response();
    response.then()
        .log()
        .all();
    return response;
  }

}
