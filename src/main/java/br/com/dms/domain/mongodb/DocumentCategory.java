package br.com.dms.domain.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import br.com.dms.domain.core.DocumentGroup;

@Document(collection = "documentCategory")
public class DocumentCategory {

	@Id
	private String id;

	private String name;

	private String description;

	@Field("document_group")
	private DocumentGroup documentGroup;

	private String prefix;

	@Field("main_type")
	private String mainType;

	@Field("type_search")
	private String typeSearch;

	@Field("unique_attributes")
	private String uniqueAttributes;

	@Field("search_duplicate_criteria")
	private String searchDuplicateCriteria;

	private String path;

	@Field("validity_in_days")
	private Long validityInDays;

	private String site;

	@Field("parent_folder")
	private String parentFolder;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public DocumentGroup getDocumentGroup() {
		return documentGroup;
	}

	public void setDocumentGroup(DocumentGroup documentGroup) {
		this.documentGroup = documentGroup;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getMainType() {
		return mainType;
	}

	public void setMainType(String mainType) {
		this.mainType = mainType;
	}

	public String getTypeSearch() {
		return typeSearch;
	}

	public void setTypeSearch(String typeSearch) {
		this.typeSearch = typeSearch;
	}

	public String getUniqueAttributes() {
		return uniqueAttributes;
	}

	public void setUniqueAttributes(String uniqueAttributes) {
		this.uniqueAttributes = uniqueAttributes;
	}

	public String getSearchDuplicateCriteria() {
		return searchDuplicateCriteria;
	}

	public void setSearchDuplicateCriteria(String searchDuplicateCriteria) {
		this.searchDuplicateCriteria = searchDuplicateCriteria;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Long getValidityInDays() {
		return validityInDays;
	}

	public void setValidityInDays(Long validityInDays) {
		this.validityInDays = validityInDays;
	}

	public String getSite() {
		return site;
	}

	public void setSite(String site) {
		this.site = site;
	}

	public String getParentFolder() {
		return parentFolder;
	}

	public void setParentFolder(String parentFolder) {
		this.parentFolder = parentFolder;
	}
}
