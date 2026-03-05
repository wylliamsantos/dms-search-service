package br.com.dms.controller;

import br.com.dms.controller.request.SearchByBusinessKeyRequest;
import br.com.dms.controller.request.SearchByCpfRequest;
import br.com.dms.controller.response.pagination.EntryPagination;
import br.com.dms.service.SearchService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/search")
@PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_ADMIN','ROLE_REVIEWER','ROLE_VIEWER','ROLE_DOCUMENT_VIEWER')")
public class SearchController {

    private final SearchService searchService;
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping(value = "/byBusinessKey", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<EntryPagination>> byBusinessKey(@RequestHeader(name = "TransactionId") String transactionId,
                                                                @RequestHeader(name = "Authorization") String authorization,
                                                                @RequestBody @Valid SearchByBusinessKeyRequest request) {
        logger.info("DMS search - TransactionId: {} - byBusinessKey type: {}", transactionId, request.getBusinessKeyType());
        return searchService.searchByBusinessKey(transactionId, authorization, request);
    }

    @GetMapping(value = "/suggestions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> suggestions(@RequestHeader(name = "TransactionId") String transactionId,
                                                    @RequestParam(name = "q") String query,
                                                    @RequestParam(name = "categories", required = false) List<String> categories,
                                                    @RequestParam(name = "limit", required = false) Integer limit) {
        logger.info("DMS search - TransactionId: {} - suggestions query={}", transactionId, query);
        return searchService.suggestions(transactionId, query, categories, limit);
    }

    // legado temporário para manter compatibilidade até remover clientes antigos
    @PostMapping(value = "/byCpf", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<EntryPagination>> byCpf(@RequestHeader(name = "TransactionId") String transactionId,
                                                       @RequestHeader(name = "Authorization") String authorization,
                                                       @RequestBody @Valid SearchByCpfRequest request) {
        logger.warn("DMS search - TransactionId: {} - endpoint legado byCpf chamado", transactionId);
        return searchService.searchByCpf(transactionId, authorization, request);
    }
}
