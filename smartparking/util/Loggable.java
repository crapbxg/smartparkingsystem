package util;

public interface Loggable {
    void logActivity(String message, Object... args);
    void logActivityWithTags(String tag, String message, Object... args);
}
