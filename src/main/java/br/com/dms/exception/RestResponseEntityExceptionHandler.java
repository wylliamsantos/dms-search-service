package br.com.dms.exception;

import org.apache.tomcat.util.http.fileupload.impl.SizeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

	// 413 MultipartException - file size too big
	@ExceptionHandler(MultipartException.class)
	public ResponseEntity<Object> handleSizeExceededException(final WebRequest request, final MultipartException ex) {
		Throwable cause = ex.getCause();
		if (cause instanceof IllegalStateException) {
			Throwable cause2 = cause.getCause();
			if (cause2 instanceof SizeException) {
				// this is tomcat specific

				HttpHeaders httpHeaders = new HttpHeaders();
				httpHeaders.setContentType(MediaType.APPLICATION_JSON);
				return handleExceptionInternal(ex, "", httpHeaders, HttpStatus.PAYLOAD_TOO_LARGE, request);
			}

		}
		ex.printStackTrace();
		return handleExceptionInternal(ex, ex, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
	}

	// 422 Generic error for business
	@ExceptionHandler(DmsBusinessException.class)
	public ResponseEntity<Object> handleDmsBusinessException(final WebRequest request, final DmsBusinessException ex) {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		DefaultError defaultError = new DefaultError(ex.getMessage(), ex.getType(), ex.getTransactionId());
		return handleExceptionInternal(ex, defaultError, httpHeaders, HttpStatus.EXPECTATION_FAILED, request);

	}

	// 500 Erro geral da API
	@ExceptionHandler(DmsException.class)
	public ResponseEntity<Object> handleDmsException(final WebRequest request, final DmsException ex) {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		DefaultError defaultError = new DefaultError(ex.getMessage(), ex.getType(), ex.getTransactionId());
		return handleExceptionInternal(ex, defaultError, httpHeaders, HttpStatus.INTERNAL_SERVER_ERROR, request);

	}

}