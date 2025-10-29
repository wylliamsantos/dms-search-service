package br.com.dms.controller.request;

import br.com.dms.domain.core.SearchScope;
import br.com.dms.domain.core.VersionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Objects;

public class SearchByCpfRequest {

    private SearchScope searchScope;
    private VersionType versionType;

    @NotBlank
    private String cpf;

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

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public List<String> getDocumentCategoryNames() {
        return documentCategoryNames;
    }

    public void setDocumentCategoryNames(List<String> documentCategoryNames) {
        this.documentCategoryNames = documentCategoryNames;
    }

    public VersionType getVersionType() {
        return Objects.isNull(versionType) ? VersionType.MAJOR : versionType;
    }

    public void setVersionType(VersionType versionType) {
        this.versionType = versionType;
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
