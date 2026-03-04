package br.com.dms.repository.mongo;

import br.com.dms.domain.mongodb.DmsDocument;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface DmsDocumentRepositoryCustom {

    List<DmsDocument> searchByTenantCategoryAndText(String tenantId,
                                                    List<String> categories,
                                                    String textQuery,
                                                    Sort sort);
}
