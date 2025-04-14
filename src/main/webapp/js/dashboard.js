document.addEventListener("DOMContentLoaded", () => {
    const userInfoHeader = document.getElementById("user-info-header");
    const fixedLogoutButton = document.getElementById("fixed-logout-btn");

    const ROLE_ENDUSER = "ENDUSER";
    const ROLE_PARTNER = "PARTNER";
    const ROLE_BACKOFFICE = "BACKOFFICE";
    const ROLE_ADMIN = "ADMIN";

    function getTokenData() {
        const token = localStorage.getItem("token");
        const role = localStorage.getItem("userRole");
        const email = localStorage.getItem("userEmail");
        const expiry = localStorage.getItem("tokenExpiry");
        if (!token || !role || !email || !expiry) {
            return null;
        }
        return { token, role, email, expiry };
    }

    function clearStoredSessionData() {
        localStorage.removeItem("token");
        localStorage.removeItem("userRole");
        localStorage.removeItem("userEmail");
        localStorage.removeItem("tokenExpiry");
    }

    function redirectToLogin() {
        window.location.href = "/page/login.html";
    }

    function displayUserInfo(email, role) {
        if (userInfoHeader) {
            userInfoHeader.textContent = `Logged in as: ${email} (${role})`;
        }
    }

    function updateUIForRole(role) {
        const canModifyWorksheet = role === ROLE_PARTNER || role === ROLE_BACKOFFICE;
        const canCreateListWorksheet = role === ROLE_BACKOFFICE;
        const canAccessWorksheetSection = canModifyWorksheet || canCreateListWorksheet;

        const canAccessBackofficeUM = role === ROLE_BACKOFFICE || role === ROLE_ADMIN;
        const canAccessAdminUM = role === ROLE_ADMIN;


        const setVisibility = (selector, canAccess) => {
            document.querySelectorAll(selector).forEach(el => {
                el.style.display = canAccess ? "block" : "none";
            });
        };

        const setVisibilityForLi = (selector, canAccess) => {
            document.querySelectorAll(selector).forEach(el => {
                if (el.tagName === 'LI') {
                    el.style.display = canAccess ? "list-item" : "none";
                } else {
                    el.style.display = canAccess ? "block" : "none";
                }
            });
        };


        setVisibility("#worksheet-management-section", canAccessWorksheetSection);
        setVisibilityForLi("#worksheet-management-section .modify-ws", canModifyWorksheet);
        setVisibilityForLi("#worksheet-management-section .create-list-ws", canCreateListWorksheet);


        setVisibility("#user-management-section", canAccessBackofficeUM);
        setVisibility(".backoffice-only", canAccessBackofficeUM && !canAccessAdminUM);
        setVisibility(".admin-only", canAccessAdminUM);

    }

    async function logout() {
        const tokenData = getTokenData();
        if (tokenData && tokenData.token) {
            try {
                await fetch("/rest/logout", {
                    method: "POST",
                    headers: {
                        "Authorization": `Bearer ${tokenData.token}`,
                        "Content-Type": "application/json"
                    }
                });
            } catch (error) {
                console.error("Logout fetch failed:", error);
            }
        }
        clearStoredSessionData();
        redirectToLogin();
    }

    function initializeDashboard() {
        const tokenData = getTokenData();

        if (!tokenData) {
            redirectToLogin();
            return;
        }

        const nowSeconds = Math.floor(Date.now() / 1000);
        const expirySeconds = parseInt(tokenData.expiry, 10);

        if (isNaN(expirySeconds) || expirySeconds < nowSeconds) {
            clearStoredSessionData();
            redirectToLogin();
            return;
        }

        displayUserInfo(tokenData.email, tokenData.role);
        updateUIForRole(tokenData.role);

        if (fixedLogoutButton) {
            fixedLogoutButton.addEventListener("click", logout);
        } else {
            console.error("Fixed logout button not found");
        }
    }

    initializeDashboard();

});