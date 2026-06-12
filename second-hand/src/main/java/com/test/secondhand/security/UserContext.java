package com.test.secondhand.security;

public class UserContext {

    private static final ThreadLocal<User> CONTEXT = new ThreadLocal<>();

    public static class User {
        private Long id;
        private String username;
        private String role;

        public User() {}

        public User(Long id, String username, String role) {
            this.id = id;
            this.username = username;
            this.role = role;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    public static void setUser(Long id, String username, String role) {
        CONTEXT.set(new User(id, username, role));
    }

    public static User getUser() {
        return CONTEXT.get();
    }

    public static Long getUserId() {
        User user = CONTEXT.get();
        return user != null ? user.getId() : null;
    }

    public static String getUsername() {
        User user = CONTEXT.get();
        return user != null ? user.getUsername() : null;
    }

    public static String getRole() {
        User user = CONTEXT.get();
        return user != null ? user.getRole() : null;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
