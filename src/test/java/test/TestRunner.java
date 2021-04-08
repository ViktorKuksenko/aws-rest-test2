package test;

import java.util.Arrays;
import java.util.List;
import services.AwsService;

public class TestRunner {

  public static final String HTTP_GET_METHOD = "GET";
  public static final String HTTP_PUT_METHOD = "PUT";
  public static final String HTTP_DELETE_METHOD = "DELETE";
  public static final String X_AMZ_CONTENT_SHA_256_HEADER = "x-amz-content-sha256";
  public static final String X_AMZ_DATE_HEADER = "x-amz-date";
  public static final String HOST_HEADER = "host";
  public static final String X_AMZ_ACL_HEADER = "x-amz-acl";
  List<String> canonicalHeaders = Arrays.asList(X_AMZ_CONTENT_SHA_256_HEADER, X_AMZ_DATE_HEADER);
  List<String> signedHeaders = Arrays
      .asList(HOST_HEADER, X_AMZ_CONTENT_SHA_256_HEADER, X_AMZ_DATE_HEADER);

//  public <T> AwsService commonRequest(T model, String httpMethod) {
//    AwsService awsService = AwsService.getInstance()
//        .setHttpMethod(httpMethod)
//        .setPayload("")
//        .setCanonicalHeaders(canonicalHeaders)
//        .setSignedHeaders(signedHeaders)
//        .setHashPayloadAsAString(true);
//    if (model instanceof BucketModel) {
//      awsService.setBucketName(((BucketModel) model).getBucketName());
//    } else if (model instanceof ObjectModel) {
//      awsService.setBucketName(((ObjectModel) model).getBucketName());
//    }
//    return awsService;
//  }

  public AwsService commonRequest(String bucketName) {
    AwsService awsService = AwsService.getInstance()
        .setPayload("")
        .setCanonicalHeaders(canonicalHeaders)
        .setSignedHeaders(signedHeaders)
        .setHashPayloadAsAString(true)
        .setBucketName(bucketName);
    return awsService;
  }

}