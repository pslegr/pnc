package org.jboss.pnc.core.builder;

import org.jboss.pnc.spi.BuildResult;
import org.jboss.pnc.spi.builddriver.BuildDriverResult;
import org.jboss.pnc.spi.repositorymanager.RepositoryManagerResult;

/**
 * Created by <a href="mailto:matejonnet@gmail.com">Matej Lazar</a> on 2015-02-02.
 */
public class DefaultBuildResult implements BuildResult {

    private BuildDriverResult buildDriverResult;

    public DefaultBuildResult(BuildDriverResult buildDriverResult, RepositoryManagerResult repositoryManagerResult) {
        this.buildDriverResult = buildDriverResult;
        this.repositoryManagerResult = repositoryManagerResult;
    }

    private RepositoryManagerResult repositoryManagerResult;

    @Override
    public BuildDriverResult getBuildDriverResult() {
        return buildDriverResult;
    }

    @Override
    public RepositoryManagerResult getRepositoryManagerResult() {
        return repositoryManagerResult;
    }
}
