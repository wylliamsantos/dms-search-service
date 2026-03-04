package br.com.dms.repository.mongo;

import br.com.dms.domain.mongodb.DmsDocument;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DmsDocumentRepository extends MongoRepository<DmsDocument, String>, DmsDocumentRepositoryCustom {

    List<DmsDocument> findByTenantIdAndCategoryIn(String tenantId, List<String> categories, Sort sort);
}
