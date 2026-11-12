package phlingo.api;

final class ApiException extends RuntimeException {
    final int status;
    final String code;
    ApiException(int status, String code, String message) { super(message); this.status = status; this.code = code; }
    static ApiException bad(String message) { return new ApiException(400, "VALIDATION_ERROR", message); }
    static ApiException unauthorized() { return new ApiException(401, "AUTH_REQUIRED", "Vui lòng đăng nhập hoặc đăng nhập lại."); }
    static ApiException forbidden() { return new ApiException(403, "FORBIDDEN", "Tài khoản không có quyền thực hiện thao tác này."); }
    static ApiException missing(String what) { return new ApiException(404, "NOT_FOUND", what + " không tồn tại."); }
    static ApiException conflict(String message) { return new ApiException(409, "CONFLICT", message); }
}
