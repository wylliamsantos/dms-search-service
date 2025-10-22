package br.com.dms.controller;

import br.com.dms.controller.request.SearchByCpfRequest;
import br.com.dms.controller.response.pagination.EntryPagination;
import br.com.dms.domain.core.SearchScope;
import br.com.dms.domain.core.VersionType;
import br.com.dms.service.SearchService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/search")
public class SearchController {

    private final SearchService searchService;
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping(value = "/byAuthor", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<EntryPagination>> byAuthor(@RequestHeader(name = "TransactionId") String transactionId,
                                      @RequestHeader(name = "Authorization") String authorization,
                                      @RequestParam(name = "author") String author,
                                      @RequestParam(name = "skipCount", required = false) Integer skipCount,
                                      @RequestParam(name = "searchScope", required = false) SearchScope searchScope,
                                      @RequestParam(name = "maxItems", required = false) Integer maxItems) {
        logger.info("DMS search - TransactionId: {} - byAuthor author: {}", transactionId, author);
        return searchService.searchByAuthor(transactionId, authorization, author, skipCount, searchScope, maxItems);
    }

    @PostMapping(value = "/byMetadata", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<EntryPagination>> byMetadata(@RequestHeader(name = "TransactionId") String transactionId,
                                        @RequestHeader(name = "Authorization") String authorization,
                                        @RequestParam(name = "type") String type,
                                        @RequestParam(name = "skipCount", required = false) Integer skipCount,
                                        @RequestParam(name = "maxItems", required = false) Integer maxItems,
                                        @RequestParam(name = "metadata", required = false) String metadata,
                                        @RequestParam(name = "searchScope", required = false) SearchScope searchScope,
                                        @RequestParam(name = "versionType", required = false, defaultValue = "MAJOR") VersionType versionType) {
        logger.info("DMS search - TransactionId: {} - byMetadata type: {}", transactionId, type);
        return searchService.searchByMetadata(transactionId, authorization, type, skipCount, maxItems, metadata, searchScope, versionType);
    }

    @PostMapping(value = "/byQuery", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<EntryPagination>> byQuery(@RequestHeader(name = "TransactionId") String transactionId,
                                     @RequestHeader(name = "Authorization") String authorization,
                                     @RequestParam(name = "query") String query,
                                     @RequestParam(name = "skipCount", required = false) Integer skipCount,
                                     @RequestParam(name = "maxItems", required = false) Integer maxItems,
                                     @RequestParam(name = "versionType", required = false, defaultValue = "MAJOR") VersionType versionType) {
        logger.info("DMS search - TransactionId: {} - byQuery", transactionId);
        return searchService.searchByQuery(transactionId, authorization, query, skipCount, maxItems, versionType);
    }

    @PostMapping(value = "/byCpf", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<EntryPagination>> byCpf(@RequestHeader(name = "TransactionId") String transactionId,
                                   @RequestHeader(name = "Authorization") String authorization,
                                   @RequestBody @Valid SearchByCpfRequest request) {
        logger.info("DMS search - TransactionId: {} - byCpf cpf: {}", transactionId, request.getCpf());
        return searchService.searchByCpf(transactionId, authorization, request);
    }
}
