package br.com.dms.controller.response.pagination;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Entry implements Serializable {

	private static final long serialVersionUID = 4376686971222479592L;

	private String createdAt;
	@JsonProperty(required = false)
	private Boolean isFolder;
	@JsonProperty(required = false)
	private Boolean isFile;
	@JsonProperty(required = false)
	private Boolean isLocked;
	private String modifiedAt;
	private String name;
	private String location;
	private String id;
	private String nodeType;
	private String parentId;
	@JsonProperty(required = false)
	private Content content;
	private String[] aspectNames;
	private String version;
	private String versionType;
	private Map<String, Object> properties;

	public String getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public Boolean isFolder() {
		return isFolder;
	}

	public void setFolder(Boolean isFolder) {
		this.isFolder = isFolder;
	}

	public Boolean isFile() {
		return isFile;
	}

	public void setFile(Boolean isFile) {
		this.isFile = isFile;
	}

	public String getModifiedAt() {
		return modifiedAt;
	}

	public void setModifiedAt(String modifiedAt) {
		this.modifiedAt = modifiedAt;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getNodeType() {
		return nodeType;
	}

	public void setNodeType(String nodeType) {
		this.nodeType = nodeType;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public Boolean getIsFolder() {
		return isFolder;
	}

	public void setIsFolder(Boolean isFolder) {
		this.isFolder = isFolder;
	}

	public Boolean getIsFile() {
		return isFile;
	}

	public void setIsFile(Boolean isFile) {
		this.isFile = isFile;
	}

	public Content getContent() {
		return content;
	}

	public void setContent(Content content) {
		this.content = content;
	}

	public Boolean getIsLocked() {
		return isLocked;
	}

	public void setIsLocked(Boolean isLocked) {
		this.isLocked = isLocked;
	}

	public String[] getAspectNames() {
		return aspectNames;
	}

	public void setAspectNames(String[] aspectNames) {
		this.aspectNames = aspectNames;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getVersionType() {
		return versionType;
	}

	public void setVersionType(String versionType) {
		this.versionType = versionType;
	}
}
