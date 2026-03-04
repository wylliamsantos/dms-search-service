package br.com.dms.repository.mongo;

import br.com.dms.domain.mongodb.DmsDocument;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
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
        Query query;
        if (StringUtils.isNotBlank(textQuery)) {
            TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matching(textQuery.trim());
            query = TextQuery.queryText(textCriteria).sortByScore();
        } else {
            query = new Query();
        }

        query.addCriteria(Criteria.where("tenantId").is(tenantId));
        query.addCriteria(Criteria.where("category").in(categories));

        if (StringUtils.isBlank(textQuery) && sort != null && sort.isSorted()) {
            query.with(sort);
        }

        return mongoTemplate.find(query, DmsDocument.class);
    }
}
