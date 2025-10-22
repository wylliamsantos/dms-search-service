package br.com.dms.controller.request;

import br.com.dms.domain.core.SearchScope;
import br.com.dms.domain.core.VersionType;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Objects;

public class SearchByCpfRequest {

    private SearchScope searchScope;
    private VersionType versionType;

    @NotNull
    private String cpf;

    @NotNull
    private List<String> documentCategoryNames;

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
}
