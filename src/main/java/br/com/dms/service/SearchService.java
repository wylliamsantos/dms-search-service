package br.com.dms.service;

import br.com.dms.audit.AuditActorResolver;
import br.com.dms.audit.AuditEventMessage;
import br.com.dms.audit.AuditEventPublisher;
import br.com.dms.controller.request.SearchByBusinessKeyRequest;
import br.com.dms.controller.request.SearchByCpfRequest;
import br.com.dms.controller.response.pagination.Content;
import br.com.dms.controller.response.pagination.EntryPagination;
import br.com.dms.domain.core.SearchScope;
import br.com.dms.domain.core.UploadStatus;
import br.com.dms.domain.core.VersionType;
import br.com.dms.domain.mongodb.DocumentCategory;
import br.com.dms.domain.mongodb.DmsDocument;
import br.com.dms.domain.mongodb.DmsDocumentVersion;
import br.com.dms.repository.mongo.DocumentCategoryRepository;
import br.com.dms.repository.mongo.DmsDocumentRepository;
import br.com.dms.repository.mongo.DmsDocumentVersionRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Environment environment;
    private final DocumentCategoryRepository documentCategoryRepository;
    private final DmsDocumentRepository dmsDocumentRepository;
    private final DmsDocumentVersionRepository dmsDocumentVersionRepository;
    private final TenantContextService tenantContextService;
    private final AuditEventPublisher auditEventPublisher;
    private final AuditActorResolver auditActorResolver;

    public SearchService(Environment environment,
                         DocumentCategoryRepository documentCategoryRepository,
                         DmsDocumentRepository dmsDocumentRepository,
                         DmsDocumentVersionRepository dmsDocumentVersionRepository,
                         TenantContextService tenantContextService,
                         AuditEventPublisher auditEventPublisher,
                         AuditActorResolver auditActorResolver) {
        this.environment = environment;
        this.documentCategoryRepository = documentCategoryRepository;
        this.dmsDocumentRepository = dmsDocumentRepository;
        this.dmsDocumentVersionRepository = dmsDocumentVersionRepository;
        this.tenantContextService = tenantContextService;
        this.auditEventPublisher = auditEventPublisher;
        this.auditActorResolver = auditActorResolver;
    }

    public ResponseEntity<Page<EntryPagination>> searchByBusinessKey(String transactionId,
                                                                     String authorization,
                                                                     SearchByBusinessKeyRequest request) {
        String businessKeyType = StringUtils.trimToEmpty(request.getBusinessKeyType()).toLowerCase(Locale.ROOT);
        String businessKeyValue = StringUtils.trimToEmpty(request.getBusinessKeyValue());
        logger.info("DMS - TransactionId: {} - Start search by businessKey type={} value={} ", transactionId, businessKeyType, businessKeyValue);

        String tenantId = tenantContextService.requireTenantId(transactionId);

        List<String> requestedCategories = Optional.ofNullable(request.getDocumentCategoryNames())
            .filter(list -> !list.isEmpty())
            .orElse(Collections.emptyList());

        if (requestedCategories.isEmpty()) {
            logger.info("DMS - TransactionId: {} - No categories provided, returning empty result", transactionId);
            return ResponseEntity.ok(Page.empty());
        }

        List<DocumentCategory> categories = documentCategoryRepository.findByTenantId(tenantId).orElse(Collections.emptyList()).stream()
            .filter(category -> requestedCategories.contains(category.getName()))
            .toList();

        if (categories.isEmpty()) {
            logger.info("DMS - TransactionId: {} - No categories matched tenant catalog. Requested: {}", transactionId, requestedCategories);
            return ResponseEntity.ok(Page.empty());
        }

        List<String> categoryNames = categories.stream().map(DocumentCategory::getName).toList();

        int pageNumber = resolvePageNumber(request.getPage());
        int pageSize = resolvePageSize(request.getSize());
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        List<DmsDocument> scopedDocuments = dmsDocumentRepository.findByTenantIdAndCategoryIn(tenantId, categoryNames, sort);
        List<DmsDocument> documents = scopedDocuments.stream()
            .filter(document -> matchesBusinessKey(document, businessKeyType, businessKeyValue))
            .toList();

        List<EntryPagination> allEntries = new ArrayList<>();
        VersionType requestedVersionType = Optional.ofNullable(request.getVersionType()).orElse(VersionType.MAJOR);
        boolean loadAllVersions = VersionType.ALL.equals(request.getVersionType());

        for (DmsDocument document : documents) {
            Optional<DmsDocumentVersion> versionOptional = resolveVersion(
                tenantId,
                document.getId(),
                loadAllVersions ? null : requestedVersionType,
                request.getSearchScope()
            );
            if (versionOptional.isEmpty()) {
                continue;
            }

            DmsDocumentVersion version = versionOptional.get();
            allEntries.add(mapToEntry(document, version));
        }

        int totalElements = allEntries.size();
        int fromIndex = Math.min(pageNumber * pageSize, totalElements);
        int toIndex = Math.min(fromIndex + pageSize, totalElements);
        List<EntryPagination> pageContent = fromIndex >= totalElements ? Collections.emptyList() : allEntries.subList(fromIndex, toIndex);

        for (EntryPagination entry : pageContent) {
            Map<String, Object> attributes = new java.util.HashMap<>();
            if (entry.getNodeType() != null) attributes.put("category", entry.getNodeType());
            if (entry.getVersion() != null) attributes.put("version", entry.getVersion());
            auditEventPublisher.publish(new AuditEventMessage(
                "DOCUMENT_VIEWED",
                Instant.now(),
                auditActorResolver.resolveUserId(),
                tenantId,
                "DOCUMENT",
                entry.getId(),
                entry.getName(),
                null,
                attributes
            ));
        }

        Page<EntryPagination> page = new PageImpl<>(pageContent, pageable, totalElements);
        logger.info("DMS - TransactionId: {} - End search by businessKey type={} results={}", transactionId, businessKeyType, page.getNumberOfElements());

        return ResponseEntity.ok(page);
    }

    // compat legado
    public ResponseEntity<Page<EntryPagination>> searchByCpf(String transactionId,
                                                             String authorization,
                                                             SearchByCpfRequest request) {
        SearchByBusinessKeyRequest adapted = new SearchByBusinessKeyRequest();
        adapted.setBusinessKeyType("cpf");
        adapted.setBusinessKeyValue(request.getCpf());
        adapted.setDocumentCategoryNames(request.getDocumentCategoryNames());
        adapted.setVersionType(request.getVersionType());
        adapted.setSearchScope(request.getSearchScope());
        adapted.setPage(request.getPage());
        adapted.setSize(request.getSize());
        return searchByBusinessKey(transactionId, authorization, adapted);
    }

    private boolean matchesBusinessKey(DmsDocument document, String businessKeyType, String businessKeyValue) {
        if (StringUtils.isBlank(businessKeyType) || StringUtils.isBlank(businessKeyValue)) {
            return false;
        }
        String targetValue = StringUtils.trimToEmpty(businessKeyValue);

        Object metadataValue = Optional.ofNullable(document.getMetadata())
            .map(map -> map.get(businessKeyType))
            .orElse(null);

        if (metadataValue != null && targetValue.equalsIgnoreCase(String.valueOf(metadataValue).trim())) {
            return true;
        }

        if ("cpf".equalsIgnoreCase(businessKeyType) && StringUtils.isNotBlank(document.getCpf())) {
            return targetValue.equalsIgnoreCase(document.getCpf().trim());
        }

        return false;
    }

    private EntryPagination mapToEntry(DmsDocument document, DmsDocumentVersion version) {
        EntryPagination entry = new EntryPagination();
        entry.setId(document.getId());
        entry.setName(document.getFilename());
        entry.setLocation(document.getCategory());
        entry.setNodeType(document.getCategory());
        entry.setIsFile(true);
        entry.setIsFolder(false);
        entry.setVersionType(version.getVersionType() != null ? version.getVersionType().name() : null);
        entry.setVersion(version.getVersionNumber() != null ? version.getVersionNumber().toPlainString() : null);
        entry.setWorkflowStatus(document.getWorkflowStatus());
        entry.setCreatedAt(formatDate(version.getCreationDate()));
        entry.setModifiedAt(formatDate(Optional.ofNullable(version.getModifiedAt()).orElse(version.getCreationDate())));

        Content content = new Content();
        content.setMimeType(resolveMimeType(document, version));
        content.setMimeTypeName(content.getMimeType());
        content.setSizeInBytes(convertToInteger(version.getFileSize()));
        entry.setContent(content);

        return entry;
    }

    private Optional<DmsDocumentVersion> resolveVersion(String tenantId,
                                                        String documentId,
                                                        VersionType requestedVersionType,
                                                        SearchScope searchScope) {
        Optional<DmsDocumentVersion> latestVersion = findLastCompletedVersion(tenantId, documentId);
        if (latestVersion.isEmpty()) {
            return Optional.empty();
        }

        DmsDocumentVersion version = latestVersion.get();
        if (!matchesVersionType(version, requestedVersionType)) {
            version = dmsDocumentVersionRepository.findByTenantIdAndDmsDocumentId(tenantId, documentId)
                .orElse(Collections.emptyList())
                .stream()
                .filter(this::isCompletedUpload)
                .sorted(Comparator.comparing(DmsDocumentVersion::getVersionNumber, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .filter(candidate -> matchesVersionType(candidate, requestedVersionType))
                .findFirst()
                .orElse(null);
        }

        if (version == null) {
            return Optional.empty();
        }

        if (!matchesScope(version, searchScope)) {
            return Optional.empty();
        }

        return Optional.of(version);
    }

    private Optional<DmsDocumentVersion> findLastCompletedVersion(String tenantId, String documentId) {
        return dmsDocumentVersionRepository.findByTenantIdAndDmsDocumentId(tenantId, documentId)
            .orElse(Collections.emptyList())
            .stream()
            .filter(this::isCompletedUpload)
            .sorted(Comparator.comparing(DmsDocumentVersion::getVersionNumber, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
            .findFirst();
    }

    private boolean isCompletedUpload(DmsDocumentVersion version) {
        UploadStatus status = version.getUploadStatus();
        return status == null || UploadStatus.COMPLETED.equals(status);
    }

    private boolean matchesVersionType(DmsDocumentVersion version, VersionType requestedVersionType) {
        if (requestedVersionType == null || VersionType.ALL.equals(requestedVersionType)) {
            return true;
        }
        return requestedVersionType.equals(version.getVersionType());
    }

    private boolean matchesScope(DmsDocumentVersion version, SearchScope scope) {
        if (scope == null || SearchScope.ALL.equals(scope) || SearchScope.LATEST.equals(scope)) {
            return true;
        }

        LocalDate expirationDate = extractExpiration(version.getMetadata());
        if (expirationDate == null) {
            return true;
        }

        return switch (scope) {
            case VALID -> !LocalDate.now().isAfter(expirationDate);
            case EXPIRED -> LocalDate.now().isAfter(expirationDate);
            default -> true;
        };
    }

    private LocalDate extractExpiration(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        Object expiration = metadata.get("dataExpiracao");
        if (expiration instanceof String value && StringUtils.isNotBlank(value)) {
            try {
                return LocalDate.parse(value);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private String resolveMimeType(DmsDocument document, DmsDocumentVersion version) {
        if (StringUtils.isNotBlank(version.getMimeType())) {
            return version.getMimeType();
        }
        return document.getMimeType();
    }

    private int resolvePageSize(Integer requestedSize) {
        if (requestedSize != null && requestedSize > 0) {
            return requestedSize;
        }

        String propertyDefaultMaxItems = environment.getProperty("dms.defaultMaxItems");
        int defaultMaxItems = propertyDefaultMaxItems == null ? 1000 : Integer.parseInt(propertyDefaultMaxItems);
        return Math.max(defaultMaxItems, 1);
    }

    private int resolvePageNumber(Integer requestedPage) {
        if (requestedPage == null) {
            return 0;
        }
        return Math.max(requestedPage, 0);
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private Integer convertToInteger(Long value) {
        if (value == null) {
            return null;
        }
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return value.intValue();
    }
}
