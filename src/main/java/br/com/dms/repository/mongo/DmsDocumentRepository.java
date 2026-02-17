package br.com.dms.repository.mongo;

import br.com.dms.domain.mongodb.DmsDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DmsDocumentRepository extends MongoRepository<DmsDocument, String> {

    @Query("{ 'tenantId': ?0, 'category': { '$in': ?2 }, '$or': [ { 'cpf': ?1 }, { 'metadata.cpf': ?1 }, { 'businessKeyValue': ?1 } ] }")
    Page<DmsDocument> findByTenantIdAndCpfAndCategoryIn(String tenantId, String cpf, List<String> categories, Pageable pageable);

    @Query("{ 'tenantId': ?0, 'category': { '$in': ?2 }, '$or': [ { 'cpf': ?1 }, { 'metadata.cpf': ?1 }, { 'businessKeyValue': ?1 } ] }")
    List<DmsDocument> findByTenantIdAndCpfAndCategoryIn(String tenantId, String cpf, List<String> categories, Sort sort);
}
