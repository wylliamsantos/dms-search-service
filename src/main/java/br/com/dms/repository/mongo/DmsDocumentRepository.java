package br.com.dms.repository.mongo;

import br.com.dms.domain.mongodb.DmsDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DmsDocumentRepository extends MongoRepository<DmsDocument, String> {

    Page<DmsDocument> findByCpfAndCategoryIn(String cpf, List<String> categories, Pageable pageable);

    List<DmsDocument> findByCpfAndCategoryIn(String cpf, List<String> categories, Sort sort);
}
