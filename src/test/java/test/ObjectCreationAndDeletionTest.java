package test;

import io.restassured.response.Response;
import helpers.TimeUtils;
import helpers.XmlUtils;
import models.ObjectModel;
import models.listbucketmodels.ContentsModel;
import models.listbucketmodels.ListBucketModel;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import services.AwsService;


public class ObjectCreationAndDeletionTest extends TestRunner {

  @DataProvider
  public Object[][] uploadObjectsData() {
    return new Object[][]{
        {new ObjectModel("restassured", "C:\\Users\\vkukse\\Desktop\\EducationProjects\\"
            + "rest-onedrive\\src\\files\\data\\12mbtest.txt", String.format("/test/test%s",
            TimeUtils.getCurrentLocalTimestamp()), "text/txt")},
//        {new ObjectModel("restassured", "C:\\Users\\vkukse\\Desktop\\EducationProjects\\"
//            + "rest-onedrive\\src\\files\\data\\test.txt", String.format("/test/test%s",
//            TimeUtils.getCurrentLocalTimestamp()), "text/txt")},
//        {new ObjectModel("restassured", "C:\\Users\\vkukse\\Desktop\\EducationProjects\\"
//            + "rest-onedrive\\src\\files\\data\\100mbtest.txt", String.format("/test/test%s",
//            TimeUtils.getCurrentLocalTimestamp()), "text/txt")},
//        {new ObjectModel("restassured", "C:\\Users\\vkukse\\Desktop\\EducationProjects\\"
//            + "rest-onedrive\\src\\files\\data\\CatJPEG.jpg", String.format("/test/test%s",
//            TimeUtils.getCurrentLocalTimestamp()), "image/jpeg")},
    };
  }

  @Test(dataProvider = "uploadObjectsData")
  public void verifyObjectCreationAndDeletionTest(ObjectModel objectModel) {
    Response awsFileUploadServiceResponse = createObject(objectModel);
    awsFileUploadServiceResponse.then().assertThat().statusCode(200);

    Response awsBucketContents = getBucketContents(objectModel);
    awsBucketContents.then().assertThat().statusCode(200);

    String bucketContents = awsBucketContents.getBody().asString();
    List<ContentsModel> contentsModel = XmlUtils
        .getDeserializedXml(bucketContents, ListBucketModel.class)
        .getContentsModel();

    Assert.assertTrue(contentsModel
        .stream()
        .anyMatch(file -> objectModel.getUri().substring(1)
            .equals(file.getKey())));

    Response deleteBucketResponse = deleteBucketContents(objectModel);
    deleteBucketResponse.then().statusCode(204);
  }

  public Response createObject(ObjectModel objectModel) {
    AwsService awsService = AwsService.getInstance()
        .setPayload(objectModel.getFilePath())
        .setPathToFile(objectModel.getFilePath())
        .setCanonicalUri(objectModel.getUri())
        .setBucketName(objectModel.getBucketName())
        .setCanonicalHeaders(canonicalHeaders)
        .setSignedHeaders(signedHeaders)
        .setHashPayloadAsAString(false)
        .setContentType(objectModel.getContentType());
    return awsService.createObject();
  }

  public Response getBucketContents(ObjectModel objectModel) {
    return commonRequest(objectModel.getBucketName()).setCanonicalUri("")
        .getBucketContents();
  }

  public Response deleteBucketContents(ObjectModel objectModel) {
    return commonRequest(objectModel.getBucketName())
        .setCanonicalUri(objectModel.getUri())
        .deleteObject();
  }

}
