package br.com.dms.controller.request;

import br.com.dms.domain.core.SearchScope;
import br.com.dms.domain.core.VersionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Objects;

public class SearchByBusinessKeyRequest {

    private SearchScope searchScope;
    private VersionType versionType;

    @NotBlank
    private String businessKeyType;

    @NotBlank
    private String businessKeyValue;

    @NotEmpty
    private List<String> documentCategoryNames;

    @Min(0)
    private Integer page;

    @Min(1)
    private Integer size;

    public SearchScope getSearchScope() {
        return searchScope;
    }

    public void setSearchScope(SearchScope searchScope) {
        this.searchScope = searchScope;
    }

    public VersionType getVersionType() {
        return Objects.isNull(versionType) ? VersionType.MAJOR : versionType;
    }

    public void setVersionType(VersionType versionType) {
        this.versionType = versionType;
    }

    public String getBusinessKeyType() {
        return businessKeyType;
    }

    public void setBusinessKeyType(String businessKeyType) {
        this.businessKeyType = businessKeyType;
    }

    public String getBusinessKeyValue() {
        return businessKeyValue;
    }

    public void setBusinessKeyValue(String businessKeyValue) {
        this.businessKeyValue = businessKeyValue;
    }

    public List<String> getDocumentCategoryNames() {
        return documentCategoryNames;
    }

    public void setDocumentCategoryNames(List<String> documentCategoryNames) {
        this.documentCategoryNames = documentCategoryNames;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
