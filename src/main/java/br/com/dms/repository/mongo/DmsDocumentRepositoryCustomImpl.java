package br.com.dms.repository.mongo;

import br.com.dms.domain.mongodb.DmsDocument;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import java.util.regex.Pattern;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DmsDocumentRepositoryCustomImpl implements DmsDocumentRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public DmsDocumentRepositoryCustomImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<DmsDocument> searchByTenantCategoryAndText(String tenantId,
                                                           List<String> categories,
                                                           String textQuery,
                                                           Sort sort) {
        Query query = new Query();
        query.addCriteria(Criteria.where("tenantId").is(tenantId));
        query.addCriteria(Criteria.where("category").in(categories));

        if (StringUtils.isNotBlank(textQuery)) {
            String safe = Pattern.quote(textQuery.trim());
            String regex = ".*" + safe + ".*";
            query.addCriteria(new Criteria().orOperator(
                Criteria.where("filename").regex(regex, "i"),
                Criteria.where("category").regex(regex, "i"),
                Criteria.where("cpf").regex(regex, "i"),
                Criteria.where("ocrText").regex(regex, "i")
            ));
        } else if (sort != null && sort.isSorted()) {
            query.with(sort);
        }

        return mongoTemplate.find(query, DmsDocument.class);
    }
}
