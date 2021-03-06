package org.jboss.pnc.rest.provider;

import com.google.common.base.Preconditions;
import org.jboss.pnc.datastore.limits.RSQLPageLimitAndSortingProducer;
import org.jboss.pnc.datastore.predicates.RSQLPredicate;
import org.jboss.pnc.datastore.predicates.RSQLPredicateProducer;
import org.jboss.pnc.datastore.repositories.ProductRepository;
import org.jboss.pnc.datastore.repositories.ProductVersionRepository;
import org.jboss.pnc.model.Product;
import org.jboss.pnc.model.ProductVersion;
import org.jboss.pnc.rest.restmodel.ProductVersionRest;
import org.springframework.data.domain.Pageable;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.jboss.pnc.datastore.predicates.ProductVersionPredicates.withProductId;
import static org.jboss.pnc.datastore.predicates.ProductVersionPredicates.withProductVersionId;
import static org.jboss.pnc.rest.utils.StreamHelper.nullableStreamOf;

@Stateless
public class ProductVersionProvider {

    private ProductVersionRepository productVersionRepository;
    private ProductRepository productRepository;

    @Inject
    public ProductVersionProvider(ProductVersionRepository productVersionRepository, ProductRepository productRepository) {
        this.productVersionRepository = productVersionRepository;
        this.productRepository = productRepository;
    }

    // needed for EJB/CDI
    public ProductVersionProvider() {
    }

    public List<ProductVersionRest> getAll(Integer pageIndex, Integer pageSize, String sortingRsql, String query, Integer productId) {
        RSQLPredicate filteringCriteria = RSQLPredicateProducer.fromRSQL(ProductVersion.class, query);
        Pageable paging = RSQLPageLimitAndSortingProducer.fromRSQL(pageSize, pageIndex, sortingRsql);

        Iterable<ProductVersion> product = productVersionRepository.findAll(withProductId(productId).and(filteringCriteria.get()), paging);
        return nullableStreamOf(product)
                .map(toRestModel())
                .collect(Collectors.toList());
    }

    public ProductVersionRest getSpecific(Integer productId, Integer productVersionId) {
        ProductVersion productVersion = productVersionRepository.findOne(withProductId(productId).and(
                withProductVersionId(productVersionId)));
        if (productVersion != null) {
            return new ProductVersionRest(productVersion);
        }
        return null;
    }

    public Integer store(Integer productId, ProductVersionRest productVersionRest) {
        Product product = productRepository.findOne(productId);
        Preconditions.checkArgument(product != null, "Couldn't find product with id " + productId);

        ProductVersion productVersion = productVersionRepository.save(productVersionRest.toProductVersion(product));
        return productVersion.getId();
    }

    public void update(Integer productId, ProductVersionRest productVersionRest) {
        Product product = productRepository.findOne(productId);
        ProductVersion productVersion = productVersionRepository.findOne(productVersionRest.getId());
        Preconditions.checkArgument(productVersion != null,
                "Couldn't find Product Version with id " + productVersionRest.getId());
        Preconditions.checkArgument(product != null,
                "Couldn't find Product with id " + product.getId());
        productVersionRepository.save(productVersionRest.toProductVersion(productVersion));
    }

    private Function<ProductVersion, ProductVersionRest> toRestModel() {
        return productVersion -> new ProductVersionRest(productVersion);
    }

}
