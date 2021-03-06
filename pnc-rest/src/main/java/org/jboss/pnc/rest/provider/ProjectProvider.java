package org.jboss.pnc.rest.provider;

import com.google.common.base.Preconditions;
import org.jboss.pnc.datastore.limits.RSQLPageLimitAndSortingProducer;
import org.jboss.pnc.datastore.predicates.RSQLPredicate;
import org.jboss.pnc.datastore.predicates.RSQLPredicateProducer;
import org.jboss.pnc.datastore.repositories.ProjectRepository;
import org.jboss.pnc.model.Project;
import org.jboss.pnc.rest.restmodel.ProjectRest;
import org.springframework.data.domain.Pageable;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.jboss.pnc.datastore.predicates.ProjectPredicates.withProductId;
import static org.jboss.pnc.datastore.predicates.ProjectPredicates.withProductVersionId;
import static org.jboss.pnc.rest.utils.StreamHelper.nullableStreamOf;

@Stateless
public class ProjectProvider {

    private ProjectRepository projectRepository;

    @Inject
    public ProjectProvider(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // needed for EJB/CDI
    public ProjectProvider() {
    }

    public String getDefaultSortingField() {
        return Project.DEFAULT_SORTING_FIELD;
    }

    public List<ProjectRest> getAll(Integer pageIndex, Integer pageSize, String sortingRsql, String query) {
        RSQLPredicate filteringCriteria = RSQLPredicateProducer.fromRSQL(Project.class, query);
        Pageable paging = RSQLPageLimitAndSortingProducer.fromRSQL(pageSize, pageIndex, sortingRsql);

        return nullableStreamOf(projectRepository.findAll(filteringCriteria.get(), paging))
                .map(toRestModel())
                .collect(Collectors.toList());
    }

    public ProjectRest getSpecific(Integer id) {
        Project project = projectRepository.findOne(id);
        if (project != null) {
            return new ProjectRest(project);
        }
        return null;
    }

    public Integer store(ProjectRest projectRest) {
        Project project = projectRest.toProject();
        project = projectRepository.save(project);
        return project.getId();
    }

    public Integer update(ProjectRest projectRest) {
        Project project = projectRepository.findOne(projectRest.getId());
        Preconditions.checkArgument(project != null, "Couldn't find project with id " + projectRest.getId());
        project = projectRepository.save(projectRest.toProject());
        return project.getId();
    }

    public Function<? super Project, ? extends ProjectRest> toRestModel() {
        return project -> new ProjectRest(project);
    }

    public List<ProjectRest> getAllForProductAndProductVersion(Integer pageIndex, Integer pageSize, String sortingRsql, String query,
            Integer productId, Integer versionId) {
        RSQLPredicate filteringCriteria = RSQLPredicateProducer.fromRSQL(Project.class, query);
        Pageable paging = RSQLPageLimitAndSortingProducer.fromRSQL(pageSize, pageIndex, sortingRsql);

        return nullableStreamOf(projectRepository.findAll(
                withProductId(productId)
                        .and(withProductVersionId(versionId))
                        .and(filteringCriteria.get()), paging))
                .map(toRestModel())
                .collect(Collectors.toList());
    }
}
