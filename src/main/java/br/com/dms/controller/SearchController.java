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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/search")
@PreAuthorize("hasAuthority('ROLE_DOCUMENT_VIEWER')")
public class SearchController {

    private final SearchService searchService;
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping(value = "/byCpf", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<EntryPagination>> byCpf(@RequestHeader(name = "TransactionId") String transactionId,
                                   @RequestHeader(name = "Authorization") String authorization,
                                   @RequestBody @Valid SearchByCpfRequest request) {
        logger.info("DMS search - TransactionId: {} - byCpf cpf: {}", transactionId, request.getCpf());
        return searchService.searchByCpf(transactionId, authorization, request);
    }
}
