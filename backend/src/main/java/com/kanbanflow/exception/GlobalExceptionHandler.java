package com.kanbanflow.exception;

import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage(), "NOT_FOUND"));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> badRequest(BadRequestException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage(), "VALIDATION_ERROR"));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> malformed(Exception e) {
        return ResponseEntity.badRequest().body(new ErrorResponse("요청 형식이 올바르지 않습니다.", "VALIDATION_ERROR"));
    }

    /** ORA-12899(값이 열 크기보다 큼: BYTE 시맨틱 한글)는 400, 그 외 DB 오류는 로그 후 500. */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> dataAccess(DataAccessException e) {
        if (hasOracleCode(e, 12899)) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("입력값이 너무 깁니다. (DB 컬럼 길이 초과)", "VALIDATION_ERROR"));
        }
        log.error("DB 오류", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("서버 오류가 발생했습니다.", "INTERNAL_ERROR"));
    }

    private static boolean hasOracleCode(Throwable t, int code) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof SQLException se && se.getErrorCode() == code) {
                return true;
            }
        }
        return false;
    }
}
