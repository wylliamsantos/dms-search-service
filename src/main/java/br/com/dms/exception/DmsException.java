package br.com.dms.exception;

/**
 * Represents a exception
 * 
 *
 */
public class DmsException extends RuntimeException {

	private static final long serialVersionUID = -9087912945651122075L;

	private String message;
	private TypeException type;
	private String transactionId;

	/**
	 * 
	 * @param message
	 *            the short message
	 * 
	 * @param type
	 *            the type of exception eg., CONF, VALID...
	 */
	public DmsException(String shortMessage,TypeException type, String transactionId) {
		this.message = shortMessage;
		this.type = type;
		this.transactionId = transactionId;
	}


	public String getMessage() {
		return message;
	}

	public TypeException getType() {
		return type;
	}

	public void setType(TypeException type) {
		this.type = type;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

}