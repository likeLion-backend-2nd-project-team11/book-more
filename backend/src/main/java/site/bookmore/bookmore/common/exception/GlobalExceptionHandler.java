package site.bookmore.bookmore.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import site.bookmore.bookmore.common.dto.ErrorResponse;
import site.bookmore.bookmore.common.dto.ResultResponse;

import javax.persistence.PersistenceException;

import static site.bookmore.bookmore.common.exception.ErrorCode.DATABASE_ERROR;
import static site.bookmore.bookmore.common.exception.ErrorCode.INVALID_EMAIL_FORMAT;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(PersistenceException.class)
    public ResponseEntity<ResultResponse<ErrorResponse>> persistenceException() {
        log.error("{} {}", DATABASE_ERROR.name(), DATABASE_ERROR.getMessage());
        return ResponseEntity.status(DATABASE_ERROR.getHttpStatus())
                .body(ResultResponse.error(ErrorResponse.of(DATABASE_ERROR)));
    }

    @ExceptionHandler(AbstractAppException.class)
    public ResponseEntity<ResultResponse<ErrorResponse>> abstractBaseExceptionHandler(AbstractAppException e) {
        log.error("{} {}", e.getErrorCode().name(), e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                .body(ResultResponse.error(e));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultResponse<ErrorResponse>> handleValidationExceptions(){
        log.error("{} {}", INVALID_EMAIL_FORMAT.name(), INVALID_EMAIL_FORMAT.getMessage());
        return ResponseEntity.status(INVALID_EMAIL_FORMAT.getHttpStatus())
                .body(ResultResponse.error(ErrorResponse.of(INVALID_EMAIL_FORMAT)));
    }
}