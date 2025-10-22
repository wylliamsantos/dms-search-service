package br.com.dms.controller.response.pagination;

import java.io.Serializable;

public class Content implements Serializable {

	private static final long serialVersionUID = -1811127731881223424L;

	private String mimeType;
	private String mimeTypeName;
	private Integer sizeInBytes;
	private String encoding;

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getMimeTypeName() {
		return mimeTypeName;
	}

	public void setMimeTypeName(String mimeTypeName) {
		this.mimeTypeName = mimeTypeName;
	}

	public Integer getSizeInBytes() {
		return sizeInBytes;
	}

	public void setSizeInBytes(Integer sizeInBytes) {
		this.sizeInBytes = sizeInBytes;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

}
