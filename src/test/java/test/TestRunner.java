package test;

import services.AwsService;

public class TestRunner {

  protected AwsService commonRequest(String bucketName) {
    AwsService awsService = AwsService.getInstance()
        .setPayload("")
        .setHashPayloadAsAString(true)
        .setBucketName(bucketName);
    return awsService;
  }

}
