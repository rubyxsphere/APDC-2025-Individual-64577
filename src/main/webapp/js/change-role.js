document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("change-role-form");
    const targetEmailInput = document.getElementById("target-email");
    const newRoleSelect = document.getElementById("new-role");
    const loadingElement = document.getElementById("change-role-loading");
    const errorElement = document.getElementById("error-message-cr");
    const successElement = document.getElementById("success-message-cr");
    const userInfoElement = document.getElementById("user-info-header");
    const logoutButton = document.getElementById("fixed-logout-btn");

    let currentUserEmail = null;
    let currentUserRole = null;
    const ROLE_ADMIN = "ADMIN";
    const ROLE_BACKOFFICE = "BACKOFFICE";

    function getTokenData() {
        const token = localStorage.getItem("token");
        const role = localStorage.getItem("userRole");
        const email = localStorage.getItem("userEmail");
        const expiry = localStorage.getItem("tokenExpiry");
        if (!token || !role || !email || !expiry) {
            return null;
        }
        currentUserEmail = email;
        currentUserRole = role;
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
        if (userInfoElement) {
            userInfoElement.textContent = `Logged in as: ${email} (${role})`;
        }
    }

    function checkAuth() {
        const tokenData = getTokenData();
        if (!tokenData) {
            redirectToLogin();
            return null;
        }
        const nowSeconds = Math.floor(Date.now() / 1000);
        const expirySeconds = parseInt(tokenData.expiry, 10);

        if (isNaN(expirySeconds) || expirySeconds < nowSeconds) {
            clearStoredSessionData();
            redirectToLogin();
            return null;
        }
        displayUserInfo(tokenData.email, tokenData.role);
        if (tokenData.role !== ROLE_ADMIN && tokenData.role !== ROLE_BACKOFFICE) {
            showMessage(errorElement, "Access Denied: Insufficient permissions.", true);
            if(form) form.style.display = "none";
            return null;
        }
        return tokenData;
    }

    function showLoading() {
        if (loadingElement) loadingElement.style.display = "block";
        if (form) form.style.display = "none";
        hideMessages();
    }

    function showMessage(element, message, isError) {
        if (element) {
            element.textContent = message;
            element.style.display = "block";
            element.classList.remove(isError ? "success-message" : "error-message");
            element.classList.add(isError ? "error-message" : "success-message");
        }
        if (loadingElement) loadingElement.style.display = "none";
        if (form) form.style.display = "flex";
    }

    function hideMessages() {
        if (errorElement) errorElement.style.display = "none";
        if (successElement) successElement.style.display = "none";
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

    const initialTokenData = checkAuth();

    if (logoutButton) {
        logoutButton.addEventListener("click", logout);
    }


    if (form && initialTokenData) {
        if (loadingElement) loadingElement.style.display = "none";
        form.style.display = "flex";

        if(currentUserRole === ROLE_BACKOFFICE && newRoleSelect) {
            Array.from(newRoleSelect.options).forEach(option => {
                if (option.value === ROLE_ADMIN || option.value === ROLE_BACKOFFICE) {
                    option.disabled = true;
                }
            });
        }


        form.addEventListener("submit", async function (event) {
            event.preventDefault();
            hideMessages();

            const targetEmail = targetEmailInput.value.trim();
            const newRole = newRoleSelect.value;

            if (!targetEmail || !newRole) {
                showMessage(errorElement, "Please enter the target user's email and select a new role.", true);
                return;
            }

            if (targetEmail === currentUserEmail) {
                showMessage(errorElement, "You cannot change your own role.", true);
                return;
            }

            const currentToken = getTokenData()?.token;
            if (!currentToken) {
                showMessage(errorElement,"Session expired. Please login again.", true);
                setTimeout(redirectToLogin, 2000);
                return;
            }

            submitChangeRole({ email: targetEmail, newRole: newRole }, currentToken);
        });
    } else if (!initialTokenData) {
        if (loadingElement) loadingElement.style.display = "none";
    }

    async function submitChangeRole(payload, token) {
        showLoading();
        try {
            const response = await fetch("/rest/user/changeRole", {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(payload)
            });

            let responseBody;
            try {
                responseBody = await response.json();
            } catch (e) {
                responseBody = { error: `Operation failed. Server returned non-JSON response (Status: ${response.status})` };
            }


            if (!response.ok || !responseBody.success) {
                throw new Error(responseBody.error || `Failed to change role (${response.status})`);
            }

            showMessage(successElement, responseBody.success || "User role changed successfully!", false);
            form.reset();

        } catch (error) {
            console.error("Error changing role:", error);
            showMessage(errorElement, error.message || "An unexpected error occurred.", true);
        } finally {
            if (loadingElement) loadingElement.style.display = "none";
            if (form) form.style.display = "flex";
        }
    }
});