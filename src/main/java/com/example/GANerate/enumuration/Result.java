package com.example.GANerate.enumuration;

import lombok.Getter;

@Getter
public enum Result {

    OK(0, "성공"),
    LOGOUT_OK(0, "로그아웃 성공"),
    DELETE_OK(0, "회원 탈퇴 성공"),
    FAIL(-1, "실패"),
    BAD_REQUEST(-2,"잘못된 요청"),
    NOT_USES_TOKEN(-2, "유효한 토큰이 존재하지 않습니다."),

    //유저
    USERID_DUPLICATED(2200, "중복되는 아이디"),
    USERID_NOT_FOUND(2201, "존재하지 않는 아이디"),
    INVALID_PASSWORD(2202, "올바르지 않은 비밀번호"),
    NOT_FOUND_USER(2203, "존재하지 않는 회원"),
    UNAUTHORITY_TOKEN(2204, "권한 정보가 없는 토큰"),
    FAIL_SEND_EMAIL(2205, "인증번호 전송 실패"),
    UNCORRECT_CERTIFICATION_NUM(2206, "올바르지 않은 인증번호"),
    INVALID_REFRESH_TOKEN(2207, "올바르지 않은 refresh token"),
    UN_AUTHENTICATION_EMAIL(2208, "이메일 미인증"),

    // 데이터 상품
    NOT_FOUND_DATA_PRODUCT(3001, "존재하지 않는 데이터 상품"),

    //파일업로드
    INVALID_FILE(4001, "jpg, png 파일만 zip내부에 있을 수 있음."),
    FAIL_UPLOAD_FILE(4002, "파일 업로드 실패"),
    NO_IMAGE_FILE(4003,"zip 안에 이미지 파일이 존재하지 않음"),
    INCORRECT_FILE_TYPE_Only_JPG_JPEG_PNG(4004, "올바르지 않은 파일 확장자"),

    //좋아요
    DUPLICATED_HEART(5001, "좋아요 중복"),

    //카테고리
    NON_EXIST_CATEGORY(6001, "존재하지 않는 카테고리");




    private final int code;
    private final String message;

    Result(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public Result resolve(int code) {
        for (Result result : values()) {
            if (result.getCode() == code) {
                return result;
            }
        }
        return null;
    }

}
