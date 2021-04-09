package test;

import java.util.Arrays;
import java.util.List;
import services.AwsService;

public class TestRunner {
  public static final String X_AMZ_CONTENT_SHA_256_HEADER = "x-amz-content-sha256";
  public static final String X_AMZ_DATE_HEADER = "x-amz-date";
  public static final String HOST_HEADER = "host";
  public static final String X_AMZ_ACL_HEADER = "x-amz-acl";
  List<String> canonicalHeaders = Arrays.asList(X_AMZ_CONTENT_SHA_256_HEADER, X_AMZ_DATE_HEADER);
  List<String> signedHeaders = Arrays
      .asList(HOST_HEADER, X_AMZ_CONTENT_SHA_256_HEADER, X_AMZ_DATE_HEADER);

  protected AwsService commonRequest(String bucketName) {
    AwsService awsService = AwsService.getInstance()
        .setPayload("")
        .setCanonicalHeaders(canonicalHeaders)
        .setSignedHeaders(signedHeaders)
        .setHashPayloadAsAString(true)
        .setBucketName(bucketName);
    return awsService;
  }

}
