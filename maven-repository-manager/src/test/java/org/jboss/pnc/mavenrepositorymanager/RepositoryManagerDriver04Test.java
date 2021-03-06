package org.jboss.pnc.mavenrepositorymanager;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.commonjava.aprox.client.core.Aprox;
import org.commonjava.aprox.client.core.util.UrlUtils;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.jboss.pnc.model.Artifact;
import org.jboss.pnc.model.BuildRecordSet;
import org.jboss.pnc.model.BuildConfiguration;
import org.jboss.pnc.spi.repositorymanager.RepositoryManagerResult;
import org.jboss.pnc.spi.repositorymanager.model.RepositoryConfiguration;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class RepositoryManagerDriver04Test 
    extends AbstractRepositoryManagerDriverTest
{

    @Test
    public void persistArtifacts_ContainsTwoUploads() throws Exception {
        BuildConfiguration pbc = simpleBuildConfiguration();

        BuildRecordSet bc = new BuildRecordSet();
        bc.setProductVersion(pbc.getProductVersion());

        RepositoryConfiguration rc = driver.createRepository(pbc, bc);
        assertThat(rc, notNullValue());

        String baseUrl = rc.getConnectionInfo().getDeployUrl();
        String pomPath = "org/commonjava/aprox/aprox-core/0.17.0/aprox-core-0.17.0.pom";
        String jarPath = "org/commonjava/aprox/aprox-core/0.17.0/aprox-core-0.17.0.jar";

        CloseableHttpClient client = HttpClientBuilder.create().build();

        for (String path : new String[] { pomPath, jarPath }) {
            final String url = UrlUtils.buildUrl(baseUrl, path);

            HttpPut put = new HttpPut(url);
            put.setEntity(new StringEntity("This is a test"));

            boolean uploaded = client.execute(put, new ResponseHandler<Boolean>() {
                @Override
                public Boolean handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                    try {
                        return response.getStatusLine().getStatusCode() == 201;
                    } finally {
                        if (response instanceof CloseableHttpResponse) {
                            IOUtils.closeQuietly((CloseableHttpResponse) response);
                        }
                    }
                }
            });

            assertThat("Failed to upload: " + url, uploaded, equalTo(true));
        }

        RepositoryManagerResult repositoryManagerResult = rc.extractBuildArtifacts();

        List<Artifact> artifacts = repositoryManagerResult.getBuiltArtifacts();
        System.out.println(artifacts);

        assertThat(artifacts, notNullValue());
        assertThat(artifacts.size(), equalTo(2));

        ProjectVersionRef pvr = new ProjectVersionRef("org.commonjava.aprox", "aprox-core", "0.17.0");
        Set<String> refs = new HashSet<>();
        refs.add(new ArtifactRef(pvr, "pom", null, false).toString());
        refs.add(new ArtifactRef(pvr, "jar", null, false).toString());

        for (Artifact artifact : artifacts) {
            assertThat(artifact + " is not in the expected list of built artifacts: " + refs,
                    refs.contains(artifact.getIdentifier()),
                    equalTo(true));
        }

        Aprox aprox = driver.getAprox();

        for (String path : new String[] { pomPath, jarPath }) {
            final String url = aprox.content().contentUrl(StoreType.hosted, rc.getCollectionId(), path);
            boolean downloaded = client.execute(new HttpGet(url), new ResponseHandler<Boolean>() {
                @Override
                public Boolean handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                    try {
                        return response.getStatusLine().getStatusCode() == 200;
                    } finally {
                        if (response instanceof CloseableHttpResponse) {
                            IOUtils.closeQuietly((CloseableHttpResponse) response);
                        }
                    }
                }
            });

            assertThat("Failed to download: " + url, downloaded, equalTo(true));
        }

        client.close();

    }

}
