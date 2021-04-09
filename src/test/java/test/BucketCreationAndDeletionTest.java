package test;

import static constants.AssertConstants.*;

import helpers.TimeUtils;
import helpers.XmlUtils;
import io.restassured.response.Response;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import models.BucketModel;
import models.aclresponsebucketmodels.AccessControlModel;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import services.AwsService;

public class BucketCreationAndDeletionTest extends TestRunner {

  @DataProvider
  private Object[][] bucketCreationData() {
    return new Object[][]{
        {new BucketModel(String.format("testBucket%s", TimeUtils.getCurrentLocalTimestamp())
            ,
            "C:\\Users\\vkukse\\Desktop\\EducationProjects\\rest-onedrive\\src\\files\\data\\test.xml"
            , "application/xml", "private", Arrays.asList(FULL_CONTROL))},
        {new BucketModel(String.format("testBucket%s", TimeUtils.getCurrentLocalTimestamp())
            , "C:\\Users\\vkukse\\Desktop\\EducationProjects\\rest-onedrive\\src\\files\\data\\test.xml"
            , "application/xml", "public-read", Arrays.asList(FULL_CONTROL, READ))},
        {new BucketModel(String.format("testBucket%s", TimeUtils.getCurrentLocalTimestamp())
            , "C:\\Users\\vkukse\\Desktop\\EducationProjects\\rest-onedrive\\src\\files\\data\\test.xml"
            , "application/xml", "public-read-write", Arrays.asList(FULL_CONTROL, READ, WRITE))},
        {new BucketModel(String.format("testBucket%s", TimeUtils.getCurrentLocalTimestamp())
            , "C:\\Users\\vkukse\\Desktop\\EducationProjects\\rest-onedrive\\src\\files\\data\\test.xml"
            , "application/xml", "authenticated-read", Arrays.asList(FULL_CONTROL, READ))}
    };
  }

  @Test(dataProvider = "bucketCreationData")
  public void awsBucketCreationWithAclPermissionsAndDeletionTest(BucketModel bucketModel) {
    Response bucketCreationResponse = createBucket(bucketModel);
    bucketCreationResponse.then().assertThat().statusCode(200);
    Response aclResponse = listBucketAcl(bucketModel);
    aclResponse.then().assertThat().statusCode(200);
    String aclContents = aclResponse.getBody().asString();
    List<String> permissions = XmlUtils.getDeserializedXml(aclContents, AccessControlModel.class)
        .getGrantModels()
        .stream()
        .map(x -> x.getPermission())
        .collect(Collectors.toList());
    Assert.assertEquals(permissions, bucketModel.getAccessControlList());
    deleteBucket(bucketModel).then().assertThat().statusCode(204);
  }

  private Response createBucket(BucketModel bucketModel) {
    List<String> canonicalHeaders = Arrays
        .asList(X_AMZ_ACL_HEADER, X_AMZ_CONTENT_SHA_256_HEADER, X_AMZ_DATE_HEADER);
    List<String> signedHeaders = Arrays
        .asList(HOST_HEADER, X_AMZ_ACL_HEADER, X_AMZ_CONTENT_SHA_256_HEADER, X_AMZ_DATE_HEADER);
    AwsService awsService = AwsService.getInstance()
        .setPayload(bucketModel.getFilePath())
        .setCanonicalUri("")
        .setBucketName(bucketModel.getBucketName())
        .setCanonicalHeaders(canonicalHeaders)
        .setSignedHeaders(signedHeaders)
        .setAcl(bucketModel.getAcl())
        .setPathToFile(bucketModel.getFilePath())
        .setContentType(bucketModel.getContentType())
        .setHashPayloadAsAString(false);
    return awsService.createBucket();
  }

  private Response listBucketAcl(BucketModel bucketModel) {
    return commonRequest(bucketModel.getBucketName()).setCanonicalUri("acl=")
        .getBucketAcl();
  }

  private Response deleteBucket(BucketModel bucketModel) {
    return commonRequest(bucketModel.getBucketName()).setCanonicalUri("")
        .deleteBucket();
  }

}
