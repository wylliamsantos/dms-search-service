package br.com.dms.service;

import br.com.dms.controller.request.SearchByCpfRequest;
import br.com.dms.domain.core.DocumentGroup;
import br.com.dms.domain.core.SearchScope;
import br.com.dms.domain.core.VersionType;
import br.com.dms.domain.mongodb.DocumentCategory;
import br.com.dms.exception.DmsBusinessException;
import br.com.dms.exception.DmsException;
import br.com.dms.exception.TypeException;
import br.com.dms.controller.response.pagination.EntryPagination;
import br.com.dms.repository.mongo.DocumentCategoryRepository;
import br.com.dms.service.handler.PrefixHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    private final RestTemplate restTemplate;
    private final Environment environment;
    private final PrefixHandler prefixHandler;
    private final DocumentCategoryRepository documentCategoryRepository;
    private final ObjectMapper objectMapper;

    public SearchService(RestTemplate restTemplate,
                         Environment environment,
                         PrefixHandler prefixHandler,
                         DocumentCategoryRepository documentCategoryRepository,
                         ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.environment = environment;
        this.prefixHandler = prefixHandler;
        this.documentCategoryRepository = documentCategoryRepository;
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<Page<EntryPagination>> searchByAuthor(String transactionId,
                                            String authorization,
                                            String author,
                                            Integer skipCount,
                                            SearchScope searchScope,
                                            Integer maxItems) {
        logger.debug("DMS - TransactionId: {} - Search by author - author: {} - skipCount: {} - maxItems: {} - Authorization: {}",
                transactionId, author, skipCount, maxItems, authorization);

        validateMaxItems(maxItems, transactionId);

        QueryResult result = executeQuery(buildAuthorQuery(author), maxItems, skipCount, transactionId, VersionType.MAJOR);
        if (result.status().is2xxSuccessful()) {
            logger.debug("DMS - TransactionId: {} - Search by author successfully - Items found: {}", transactionId,
                    Optional.ofNullable(result.page()).map(Page::getTotalElements).orElse(0L));
            return ResponseEntity.ok(result.page());
        }

        logger.warn("DMS - TransactionId: {} - Wasn't possible search by author - HTTP Status: {}", transactionId, result.status().value());
        return ResponseEntity.status(result.status()).build();
    }

    public ResponseEntity<Page<EntryPagination>> searchByMetadata(String transactionId,
                                              String authorization,
                                              String type,
                                              Integer skipCount,
                                              Integer maxItems,
                                              String metadata,
                                              SearchScope searchScope,
                                              VersionType versionType) {
        logger.debug("DMS - TransactionId: {} - Search by metadata - type: {} - skipCount: {} - maxItems: {} - metadata: {} - Authorization: {}",
                transactionId, type, skipCount, maxItems, metadata, authorization);

        Map<String, String> metadataFilters = parseMetadata(metadata, transactionId);
        DocumentCategory category = loadCategory(type, transactionId);
        String query = buildCategoryQuery(category, metadataFilters, searchScope, transactionId);

        QueryResult result = executeQuery(query, maxItems, skipCount, transactionId, versionType);
        if (result.status().is2xxSuccessful()) {
            logger.debug("DMS - TransactionId: {} - Search by metadata successfully - Items found: {}", transactionId,
                    Optional.ofNullable(result.page()).map(Page::getTotalElements).orElse(0L));
            return ResponseEntity.ok(result.page());
        }

        return ResponseEntity.status(result.status()).build();
    }

    public ResponseEntity<Page<EntryPagination>> searchByQuery(String transactionId,
                                           String authorization,
                                           String query,
                                           Integer skipCount,
                                           Integer maxItems,
                                           VersionType versionType) {
        logger.debug("DMS - TransactionId: {} - Search by query - skipCount: {} - maxItems: {} - Authorization: {}",
                transactionId, skipCount, maxItems, authorization);

        QueryResult result = executeQuery(query, maxItems, skipCount, transactionId, versionType);
        if (result.status().is2xxSuccessful()) {
            logger.debug("DMS - TransactionId: {} - Search by query successfully - Items found: {}", transactionId,
                    Optional.ofNullable(result.page()).map(Page::getTotalElements).orElse(0L));
            return ResponseEntity.ok(result.page());
        }

        return ResponseEntity.status(result.status()).build();
    }

    public ResponseEntity<Page<EntryPagination>> searchByCpf(String transactionId,
                                         String authorization,
                                         SearchByCpfRequest request) {
        logger.info("DMS - TransactionId: {} - Start search by cpf - cpf: {}", transactionId, request.getCpf());

        List<DocumentCategory> categories = documentCategoryRepository.findAll().stream()
                .filter(category -> DocumentGroup.PERSONAL.equals(category.getDocumentGroup()))
                .filter(category -> request.getDocumentCategoryNames().contains(category.getName()))
                .collect(Collectors.toList());

        List<EntryPagination> aggregatedEntries = new ArrayList<>();
        categories.forEach(category -> {
            try {
                Map<String, String> metadata = Collections.singletonMap(category.getMainType(), request.getCpf());
                String query = buildCategoryQuery(category, metadata, request.getSearchScope(), transactionId);
                QueryResult result = executeQuery(query, null, 0, transactionId, request.getVersionType());
                if (result.status().is2xxSuccessful() && result.page() != null) {
                    aggregatedEntries.addAll(result.page().getContent());
                }
            } catch (Exception exception) {
                logger.error("DMS - TransactionId: {} - Error executing search by cpf - cpf: {}", transactionId, request.getCpf(), exception);
            }
        });

        int pageSize = Math.max(getDefaultMaxItems(), 1);
        PageImpl<EntryPagination> page = new PageImpl<>(aggregatedEntries, PageRequest.of(0, pageSize), aggregatedEntries.size());
        logger.info("DMS - TransactionId: {} - End search by cpf - cpf: {} documentCount: {}", transactionId, request.getCpf(), page.getNumberOfElements());

        return ResponseEntity.ok(page);
    }

    private void validateMaxItems(Integer maxItems, String transactionId) {
        Integer defaultMaxItems = getDefaultMaxItems();
        if (Objects.nonNull(maxItems) && maxItems > defaultMaxItems) {
            logger.info("DMS - TransactionId: {} - Maximum items exceeded: {}", transactionId, maxItems);
            throw new DmsBusinessException(environment.getProperty("dms.msg.maxItensExceeded"), TypeException.VALID, transactionId);
        }
    }

    private QueryResult executeQuery(String queryCmis,
                                     Integer maxItems,
                                     Integer skipCount,
                                     String transactionId,
                                     VersionType versionType) {
        validateMaxItems(maxItems, transactionId);

        Integer effectiveMaxItems = Objects.requireNonNullElse(maxItems, getDefaultMaxItems());
        Integer effectiveSkipCount = Objects.requireNonNullElse(skipCount, 0);

        ObjectNode propertiesRoot = objectMapper.createObjectNode();
        ObjectNode queryNode = objectMapper.createObjectNode();
        ObjectNode pagingNode = objectMapper.createObjectNode();
        ArrayNode include = objectMapper.createArrayNode();

        include.add("properties");
        queryNode.put("query", queryCmis);
        queryNode.put("language", "cmis");
        pagingNode.put("maxItems", effectiveMaxItems);
        pagingNode.put("skipCount", effectiveSkipCount);

        propertiesRoot.set("query", queryNode);
        propertiesRoot.set("paging", pagingNode);
        propertiesRoot.set("include", include);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("authorization", environment.getProperty("dms.authorization"));
        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            logger.debug("DMS - TransactionId: {} - Running query - URL: {} - Query: {}", transactionId,
                    environment.getProperty("dms.url.search"), queryCmis);
            HttpEntity<String> httpEntity = new HttpEntity<>(propertiesRoot.toString(), httpHeaders);
            ResponseEntity<String> response = restTemplate.exchange(environment.getProperty("dms.url.search"), HttpMethod.POST, httpEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return new QueryResult(HttpStatus.OK, mapToPage(response.getBody(), versionType, transactionId));
            }

            logger.warn("DMS - TransactionId: {} - Wasn't possible search - HTTP Status: {}", transactionId, response.getStatusCodeValue());
            return new QueryResult(response.getStatusCode(), null);
        } catch (HttpClientErrorException clientErrorException) {
            logger.error("DMS - TransactionId: {} - Error to search - HTTP Status: {} - Body message: {}", transactionId,
                    clientErrorException.getRawStatusCode(), clientErrorException.getResponseBodyAsString(), clientErrorException);
            return new QueryResult(clientErrorException.getStatusCode(), null);
        } catch (Exception exception) {
            if (exception instanceof HttpServerErrorException httpServerErrorException && logger.isErrorEnabled()) {
                logger.error("DMS - TransactionId: {} - Error to search. Error body: {}", transactionId,
                        httpServerErrorException.getResponseBodyAsString(), httpServerErrorException);
            } else {
                logger.error("DMS - TransactionId: {} - Error to search", transactionId, exception);
            }
            throw new DmsException(environment.getProperty("dms.msg.unknowError"), TypeException.CONFIG, transactionId);
        }
    }

    private Page<EntryPagination> mapToPage(String body,
                                          VersionType versionType,
                                          String transactionId) {
        try {
            JsonNode rootNode = objectMapper.readTree(body);
            JsonNode paginationNode = rootNode.path("list").path("pagination");
            JsonNode entriesNode = rootNode.path("list").path("entries");

            List<EntryPagination> entries = new ArrayList<>();
            for (JsonNode jsonNode : entriesNode.findValues("entry", new ArrayList<>())) {
                EntryPagination entry = objectMapper.readValue(jsonNode.toString(), EntryPagination.class);
                if (filterVersion(versionType, entry)) {
                    entries.add(entry);
                }
            }

            int count = paginationNode.path("count").asInt(entries.size());
            int maxItems = paginationNode.path("maxItems").asInt(Math.max(count, 1));
            int skip = paginationNode.path("skipCount").asInt(0);
            long total = paginationNode.path("totalItems").asLong(entries.size());

            int pageSize = Math.max(maxItems, 1);
            int pageNumber = pageSize == 0 ? 0 : skip / pageSize;

            PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);
            return new PageImpl<>(entries, pageRequest, total);
        } catch (Exception exception) {
            logger.error("DMS - TransactionId: {} - Error to parse search response", transactionId, exception);
            throw new DmsException(environment.getProperty("dms.msg.errorSearch"), TypeException.CONFIG, transactionId);
        }
    }

    private boolean filterVersion(VersionType versionType, EntryPagination entry) {
        // Se o tipo solicitado for ALL, incluir todos os documentos
        if (VersionType.ALL.equals(versionType)) {
            return true;
        }

        // Se o documento não tem versionType definido, incluir apenas se solicitado ALL
        if (entry.getVersionType() == null) {
            return false;
        }

        // Filtrar baseado no versionType do documento vs o solicitado
        return versionType.name().equals(entry.getVersionType());
    }

    private Map<String, String> parseMetadata(String metadata, String transactionId) {
        if (StringUtils.isBlank(metadata)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(metadata, new TypeReference<HashMap<String, String>>() {
            });
        } catch (Exception exception) {
            logger.warn("DMS - TransactionId: {} - Fail to parse json input metadata: {}", transactionId, metadata);
            throw new DmsBusinessException(environment.getProperty("dms.msg.jsonMetadataError"), TypeException.VALID, transactionId);
        }
    }

    private DocumentCategory loadCategory(String type, String transactionId) {
        return documentCategoryRepository.findByName(type)
                .orElseThrow(() -> {
                    logger.info("DMS - TransactionId: {} - Type invalid {}", transactionId, type);
                    return new DmsBusinessException(environment.getProperty("dms.msg.typeInvalid"), TypeException.VALID, transactionId);
                });
    }

    private String buildAuthorQuery(String author) {
        return String.format(environment.getProperty("dms.search.byAuthor"), author);
    }

    private String buildCategoryQuery(DocumentCategory category,
                                      Map<String, String> metadata,
                                      SearchScope searchScope,
                                      String transactionId) {
        if (category.getMainType() == null) {
            logger.info("DMS - TransactionId: {} - Type invalid {}", transactionId, category.getName());
            throw new DmsBusinessException(environment.getProperty("dms.msg.typeInvalid"), TypeException.VALID, transactionId);
        }

        StringBuilder queryBuilder = new StringBuilder(resolveCategoryQueryTemplate(category, transactionId));
        metadata.forEach((key, value) -> addCriteria(queryBuilder, "and", key, "=", value));

        if (searchScope != null) {
            switch (searchScope) {
                case VALID:
                    queryBuilder.append(" and ( ");
                    queryBuilder.append(" td.").append(prefixHandler.handle(category.getPrefix(), "dataExpiracao")).append(" is null ");
                    addCriteria(queryBuilder, "or", prefixHandler.handle(category.getPrefix(), "dataExpiracao"), ">=", LocalDate.now().toString());
                    queryBuilder.append(" ) ");
                    break;
                case EXPIRED:
                    addCriteria(queryBuilder, "and", prefixHandler.handle(category.getPrefix(), "dataExpiracao"), "<", LocalDate.now().toString());
                    break;
                default:
                    break;
            }
        }

        return queryBuilder.toString();
    }

    private String resolveCategoryQueryTemplate(DocumentCategory category, String transactionId) {
        String queryTemplateV3 = environment.getProperty("dms.search.type-v3");
        if (StringUtils.isNotBlank(queryTemplateV3)) {
            try {
                return String.format(queryTemplateV3, category.getTypeSearch(), category.getName());
            } catch (IllegalFormatException ignored) {
                // Fallback to legacy template when placeholders do not match
            }
        }
        String queryTemplate = environment.getProperty("dms.search.type");
        if (StringUtils.isBlank(queryTemplate)) {
            throw new DmsException(environment.getProperty("dms.msg.errorSearch"), TypeException.CONFIG, transactionId);
        }
        return String.format(queryTemplate, category.getTypeSearch());
    }

    private void addCriteria(StringBuilder queryCmis,
                             String logicalOperator,
                             String key,
                             String arithmeticOperator,
                             String value) {
        queryCmis.append(" ").append(logicalOperator).append(" ");
        queryCmis.append("td.");
        queryCmis.append(key);
        queryCmis.append(" ").append(arithmeticOperator).append(" '");
        queryCmis.append(value);
        queryCmis.append("' ");
    }

    private Integer getDefaultMaxItems() {
        String propertyDefaultMaxItems = environment.getProperty("dms.defaultMaxItems");
        return propertyDefaultMaxItems == null ? 1000 : Integer.parseInt(propertyDefaultMaxItems);
    }

    private record QueryResult(HttpStatusCode status, Page<EntryPagination> page) {
    }
}
