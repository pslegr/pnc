package org.jboss.pnc.integration;

import static com.jayway.restassured.RestAssured.given;
import static org.jboss.pnc.integration.env.IntegrationTestEnv.getHttpPort;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.pnc.integration.assertions.ResponseAssertion;
import org.jboss.pnc.integration.deployments.Deployments;
import org.jboss.pnc.integration.matchers.JsonMatcher;
import org.jboss.pnc.integration.template.JsonTemplateBuilder;
import org.jboss.pnc.rest.endpoint.BuildRecordEndpoint;
import org.jboss.pnc.rest.endpoint.BuildRecordSetEndpoint;
import org.jboss.pnc.rest.provider.BuildRecordProvider;
import org.jboss.pnc.rest.provider.BuildRecordSetProvider;
import org.jboss.pnc.rest.restmodel.BuildRecordRest;
import org.jboss.pnc.rest.restmodel.BuildRecordSetRest;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;

@RunWith(Arquillian.class)
public class BuildRecordSetRestTest {

    public static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String PRODUCT_REST_ENDPOINT = "/pnc-web/rest/product/";
    private static final String PRODUCT_VERSION_REST_ENDPOINT = "/pnc-web/rest/product/%d/version/";
    private static final String BUILD_RECORD_REST_ENDPOINT = "/pnc-web/rest/record/";

    private static final String BUILD_RECORD_SET_REST_ENDPOINT = "/pnc-web/rest/recordset/";
    private static final String BUILD_RECORD_SET_SPECIFIC_REST_ENDPOINT = "/pnc-web/rest/recordset/%d";
    private static final String BUILD_RECORD_SET_PRODUCT_VERSION_REST_ENDPOINT = "/pnc-web/rest/recordset/productversion/%d";
    private static final String BUILD_RECORD_SET_BUILD_RECORD_REST_ENDPOINT = "/pnc-web/rest/recordset/record/%d";

    private static int productId;
    private static String productVersionName;
    private static int productVersionId;
    private static String buildRecordBuildScript;
    private static String buildRecordName;
    private static int buildRecordId;
    private static int newBuildRecordSetId;

    @Deployment(testable = false)
    public static EnterpriseArchive deploy() {
        EnterpriseArchive enterpriseArchive = Deployments.baseEar();

        JavaArchive restJar = enterpriseArchive.getAsType(JavaArchive.class, "/pnc-rest.jar");
        restJar.addClass(BuildRecordSetProvider.class);
        restJar.addClass(BuildRecordSetEndpoint.class);
        restJar.addClass(BuildRecordSetRest.class);
        restJar.addClass(BuildRecordProvider.class);
        restJar.addClass(BuildRecordEndpoint.class);
        restJar.addClass(BuildRecordRest.class);

        logger.info(enterpriseArchive.toString(true));
        return enterpriseArchive;
    }

    @Test
    @InSequence(-1)
    public void prepareBaseData() {

        given().contentType(ContentType.JSON).port(getHttpPort()).when().get(PRODUCT_REST_ENDPOINT).then().statusCode(200)
                .body(JsonMatcher.containsJsonAttribute("[0].id", value -> productId = Integer.valueOf(value)));

        Response responseProdVer = given().contentType(ContentType.JSON).port(getHttpPort()).when()
                .get(String.format(PRODUCT_VERSION_REST_ENDPOINT, productId));

        ResponseAssertion.assertThat(responseProdVer).hasStatus(200);
        productVersionId = responseProdVer.body().jsonPath().getInt("[0].id");
        productVersionName = responseProdVer.body().jsonPath().getString("[0].version");

        Response responseBuildRec = given().contentType(ContentType.JSON).port(getHttpPort()).when()
                .get(BUILD_RECORD_REST_ENDPOINT);
        ResponseAssertion.assertThat(responseBuildRec).hasStatus(200);

        buildRecordId = responseBuildRec.body().jsonPath().getInt("[0].id");
        buildRecordBuildScript = responseBuildRec.body().jsonPath().getString("[0].buildScript");
        buildRecordName = responseBuildRec.body().jsonPath().getString("[0].name");

        logger.info("productVersionId: {} ", productVersionId);
        logger.info("productVersionName: {} ", productVersionName);
        logger.info("buildRecordId: {} ", buildRecordId);
        logger.info("buildRecordBuildScript: {} ", buildRecordBuildScript);
        logger.info("buildRecordName: {} ", buildRecordName);
    }

    @Test
    @InSequence(1)
    public void shouldCreateNewBuildRecordSet() throws IOException {
        JsonTemplateBuilder buildRecordSetTemplate = JsonTemplateBuilder.fromResource("buildRecordSet_template");
        buildRecordSetTemplate.addValue("_productVersionId", String.valueOf(productVersionId));
        buildRecordSetTemplate.addValue("_buildRecordIds", String.valueOf(buildRecordId));

        Response response = given().body(buildRecordSetTemplate.fillTemplate()).contentType(ContentType.JSON)
                .port(getHttpPort()).when().post(BUILD_RECORD_SET_REST_ENDPOINT);

        ResponseAssertion.assertThat(response).hasStatus(201).hasLocationMatches(".*\\/pnc-web\\/rest\\/recordset\\/\\d+");

        String location = response.getHeader("Location");
        newBuildRecordSetId = Integer.valueOf(location.substring(location.lastIndexOf("/") + 1));
        logger.info("Created id of BuildRecordSet: " + newBuildRecordSetId);
    }

    @Test
    @InSequence(2)
    public void shouldGetBuildRecordSets() {

        Response response = given().contentType(ContentType.JSON).port(getHttpPort()).when()
                .get(BUILD_RECORD_SET_REST_ENDPOINT);
        ResponseAssertion.assertThat(response).hasStatus(200);
        ResponseAssertion.assertThat(response).hasJsonValueEqual("[0].id", newBuildRecordSetId);
    }

    @Test
    @InSequence(3)
    public void shouldGetSpecificBuildRecordSet() {

        Response response = given().contentType(ContentType.JSON).port(getHttpPort()).when()
                .get(String.format(BUILD_RECORD_SET_SPECIFIC_REST_ENDPOINT, newBuildRecordSetId));

        ResponseAssertion.assertThat(response).hasStatus(200);
        ResponseAssertion.assertThat(response).hasJsonValueEqual("id", newBuildRecordSetId);
    }

    @Test
    @InSequence(4)
    public void shouldGetBuildRecordForProductVersion() {

        Response response = given().contentType(ContentType.JSON).port(getHttpPort()).when()
                .get(String.format(BUILD_RECORD_SET_PRODUCT_VERSION_REST_ENDPOINT, productVersionId));

        ResponseAssertion.assertThat(response).hasStatus(200);
        ResponseAssertion.assertThat(response).hasJsonValueEqual("[0].id", newBuildRecordSetId);
    }

    @Test
    @InSequence(5)
    public void shouldGetBuildRecordForBuildRecord() {

        Response response = given().contentType(ContentType.JSON).port(getHttpPort()).when()
                .get(String.format(BUILD_RECORD_SET_BUILD_RECORD_REST_ENDPOINT, buildRecordId));

        ResponseAssertion.assertThat(response).hasStatus(200);
        ResponseAssertion.assertThat(response).hasJsonValueEqual("[0].id", newBuildRecordSetId);
    }

}