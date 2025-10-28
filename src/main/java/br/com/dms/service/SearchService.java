package br.com.dms.service;

import br.com.dms.controller.request.SearchByCpfRequest;
import br.com.dms.controller.response.pagination.Content;
import br.com.dms.controller.response.pagination.EntryPagination;
import br.com.dms.domain.core.DocumentGroup;
import br.com.dms.domain.core.SearchScope;
import br.com.dms.domain.core.VersionType;
import br.com.dms.domain.mongodb.DocumentCategory;
import br.com.dms.domain.mongodb.DmsDocument;
import br.com.dms.domain.mongodb.DmsDocumentVersion;
import br.com.dms.exception.DmsBusinessException;
import br.com.dms.exception.TypeException;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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

    public SearchService(Environment environment,
                         DocumentCategoryRepository documentCategoryRepository,
                         DmsDocumentRepository dmsDocumentRepository,
                         DmsDocumentVersionRepository dmsDocumentVersionRepository) {
        this.environment = environment;
        this.documentCategoryRepository = documentCategoryRepository;
        this.dmsDocumentRepository = dmsDocumentRepository;
        this.dmsDocumentVersionRepository = dmsDocumentVersionRepository;
    }

    public ResponseEntity<Page<EntryPagination>> searchByCpf(String transactionId,
                                                             String authorization,
                                                             SearchByCpfRequest request) {
        logger.info("DMS - TransactionId: {} - Start search by cpf - cpf: {}", transactionId, request.getCpf());

        validateCpf(request.getCpf(), transactionId);

        List<String> requestedCategories = Optional.ofNullable(request.getDocumentCategoryNames())
            .filter(list -> !list.isEmpty())
            .orElse(Collections.emptyList());

        if (requestedCategories.isEmpty()) {
            logger.info("DMS - TransactionId: {} - No categories provided, returning empty result", transactionId);
            return ResponseEntity.ok(Page.empty());
        }

        List<DocumentCategory> categories = documentCategoryRepository.findAll().stream()
            .filter(category -> DocumentGroup.PERSONAL.equals(category.getDocumentGroup()))
            .filter(category -> requestedCategories.contains(category.getName()))
            .toList();

        if (categories.isEmpty()) {
            logger.info("DMS - TransactionId: {} - No categories matched PERSONAL group. Requested: {}", transactionId, requestedCategories);
            return ResponseEntity.ok(Page.empty());
        }

        List<String> categoryNames = categories.stream()
            .map(DocumentCategory::getName)
            .toList();

        Pageable pageable = PageRequest.of(0, resolvePageSize(), Sort.by(Sort.Direction.DESC, "id"));
        Page<DmsDocument> documentsPage = dmsDocumentRepository.findByCpfAndCategoryIn(request.getCpf(), categoryNames, pageable);

        List<EntryPagination> entries = new ArrayList<>();
        VersionType requestedVersionType = Optional.ofNullable(request.getVersionType()).orElse(VersionType.MAJOR);
        boolean loadAllVersions = VersionType.ALL.equals(request.getVersionType());

        for (DmsDocument document : documentsPage.getContent()) {
            Optional<DmsDocumentVersion> versionOptional = resolveVersion(
                document.getId(),
                loadAllVersions ? null : requestedVersionType,
                request.getSearchScope()
            );
            if (versionOptional.isEmpty()) {
                continue;
            }

            DmsDocumentVersion version = versionOptional.get();
            entries.add(mapToEntry(document, version));
        }

        Page<EntryPagination> page = new PageImpl<>(entries, pageable, documentsPage.getTotalElements());
        logger.info("DMS - TransactionId: {} - End search by cpf - cpf: {} documentCount: {}", transactionId, request.getCpf(), page.getNumberOfElements());

        return ResponseEntity.ok(page);
    }

    private void validateCpf(String cpf, String transactionId) {
        if (StringUtils.isBlank(cpf)) {
            throw new DmsBusinessException("CPF não informado", TypeException.VALID, transactionId);
        }
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
        entry.setCreatedAt(formatDate(version.getCreationDate()));
        entry.setModifiedAt(formatDate(Optional.ofNullable(version.getModifiedAt()).orElse(version.getCreationDate())));

        Content content = new Content();
        content.setMimeType(resolveMimeType(document, version));
        content.setMimeTypeName(content.getMimeType());
        content.setSizeInBytes(convertToInteger(version.getFileSize()));
        entry.setContent(content);

        return entry;
    }

    private Optional<DmsDocumentVersion> resolveVersion(String documentId,
                                                        VersionType requestedVersionType,
                                                        SearchScope searchScope) {
        Optional<DmsDocumentVersion> latestVersion = dmsDocumentVersionRepository.findLastVersionByDmsDocumentId(documentId);
        if (latestVersion.isEmpty()) {
            return Optional.empty();
        }

        DmsDocumentVersion version = latestVersion.get();
        if (!matchesVersionType(version, requestedVersionType)) {
            version = dmsDocumentVersionRepository.findByDmsDocumentId(documentId)
                .orElse(Collections.emptyList())
                .stream()
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

    private Integer resolvePageSize() {
        String propertyDefaultMaxItems = environment.getProperty("dms.defaultMaxItems");
        int defaultMaxItems = propertyDefaultMaxItems == null ? 1000 : Integer.parseInt(propertyDefaultMaxItems);
        return Math.max(defaultMaxItems, 1);
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
