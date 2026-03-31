package com.briefin.global.apipayload.code.status;

import com.briefin.global.apipayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements BaseCode {

    // COMMON
    BAD_REQUEST(400, "COMMON400", "잘못된 요청입니다."),
    VALIDATION_ERROR(400, "VALIDATION400", "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(401, "COMMON401", "인증이 필요합니다."),
    FORBIDDEN(403, "COMMON403", "접근 권한이 없습니다."),
    NOT_FOUND(404, "COMMON404", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(405, "COMMON405", "허용되지 않은 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(500, "COMMON500", "서버 에러입니다."),
    SERVICE_UNAVAILABLE(503, "COMMON503", "서비스 이용이 일시적으로 불가능합니다."),

    // AUTH
    AUTH_UNAUTHORIZED(401, "AUTH401", "로그인 후 이용해주세요."),
    INVALID_LOGIN(401, "AUTH402", "이메일 또는 비밀번호가 올바르지 않습니다."),
    TOKEN_EXPIRED(401, "TOKEN_EXPIRED", "토큰이 만료되었습니다."),
    TOKEN_INVALID(401, "TOKEN_INVALID", "유효하지 않은 토큰입니다."),
    TOKEN_MISSING(401, "TOKEN_MISSING", "토큰이 존재하지 않습니다."),
    AUTH_FORBIDDEN(403, "AUTH403", "접근 권한이 없습니다."),
    ACCESS_DENIED(403, "ACCESS_DENIED", "해당 리소스에 접근할 수 없습니다."),
    INVALID_TOKEN(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    TOKEN_REUSE_DETECTED(401, "TOKEN_REUSE_DETECTED", "토큰 재사용이 감지되었습니다."),

    // USER
    USER_BAD_REQUEST(400, "USER400", "잘못된 사용자 요청입니다."),
    USER_NOT_FOUND(404, "USER404", "해당 사용자를 찾을 수 없습니다."),
    USER_CONFLICT(409, "USER409", "이미 존재하는 사용자입니다."),
    DUPLICATE_EMAIL(409, "DUPLICATE_EMAIL", "이미 가입된 이메일입니다. 로그인해 주세요"),
    DUPLICATE_NICKNAME(409, "DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다."),
    PASSWORD_MISMATCH(400, "PASSWORD_MISMATCH", "비밀번호가 일치하지 않습니다."),

    // FILE
    FILE_BAD_REQUEST(400, "FILE400", "파일 요청이 잘못되었습니다."),
    FILE_TOO_LARGE(413, "FILE413", "파일 크기가 너무 큽니다."),
    FILE_UNSUPPORTED_TYPE(415, "FILE415", "지원하지 않는 파일 형식입니다."),
    FILE_UPLOAD_FAIL(500, "FILE_UPLOAD_FAIL", "파일 업로드에 실패했습니다."),

    // SERVER
    DB_ERROR(500, "DB_ERROR", "데이터베이스 오류가 발생했습니다."),
    INTERNAL_ERROR(500, "INTERNAL_ERROR", "내부 서버 오류입니다."),
    SERVER_BUSY(503, "SERVER_BUSY", "서버가 과부하 상태입니다."),

    // BUSINESS
    INVALID_STATE(400, "INVALID_STATE", "잘못된 상태입니다."),
    NOT_ALLOWED_ACTION(403, "NOT_ALLOWED_ACTION", "수행할 수 없는 작업입니다."),
    ALREADY_PROCESSED(409, "ALREADY_PROCESSED", "이미 처리된 요청입니다."),

    // NEWS
    NEWS_NOT_FOUND(404, "NEWS404", "해당 뉴스를 찾을 수 없습니다."),
    NEWS_ALREADY_SCRAPED(409, "NEWS_SCRAP409", "이미 스크랩된 뉴스입니다."),
    NEWS_SCRAP_NOT_FOUND(404, "NEWS_SCRAP404", "스크랩 정보를 찾을 수 없습니다."),

    // DISCLOSURE
    DISCLOSURE_NOT_FOUND(404, "DISCLOSURE404", "해당 공시를 찾을 수 없습니다."),
    COMPANY_NOT_FOUND(404, "COMPANY404", "해당 기업을 찾을 수 없습니다.");

    private final int httpStatus;
    private final String code;
    private final String message;
}
