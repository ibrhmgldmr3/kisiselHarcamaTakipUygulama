package com.expenseapp.util;

import com.expenseapp.model.User;

/** Giriş yapan kullanıcıyı uygulama boyunca tutar; sorgular bu kullanıcının id'si ile sınırlandırılır. */
public final class SessionManager {

    private static User currentUser;

    private SessionManager() {
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
    }
}
