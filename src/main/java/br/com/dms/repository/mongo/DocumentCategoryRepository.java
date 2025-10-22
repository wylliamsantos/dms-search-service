package br.com.dms.repository.mongo;

import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import br.com.dms.domain.mongodb.DocumentCategory;

@CacheConfig(cacheNames = "documentCategory")
@Repository
public interface DocumentCategoryRepository extends MongoRepository<DocumentCategory, String> {

	@Cacheable
	Optional<DocumentCategory> findByName(String documentCategoryName);
	
}
