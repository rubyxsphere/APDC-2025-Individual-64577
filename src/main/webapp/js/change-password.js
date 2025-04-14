document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("change-password-form");
    const loadingElement = document.getElementById("change-password-loading");
    const errorElement = document.getElementById("error-message-cp");
    const successElement = document.getElementById("success-message-cp");
    const currentPasswordInput = document.getElementById("currentPassword");
    const newPasswordInput = document.getElementById("newPassword");
    const confirmNewPasswordInput = document.getElementById("confirmNewPassword");
    const userInfoElement = document.getElementById("user-info-header");
    const logoutButton = document.getElementById("fixed-logout-btn");

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
        if (userInfoElement) {
            userInfoElement.textContent = `Logged in as: ${email} (${role})`;
        }
    }

    function checkAuth() {
        const tokenData = getTokenData();
        if (!tokenData) {
            redirectToLogin();
            return false;
        }
        const nowSeconds = Math.floor(Date.now() / 1000);
        const expirySeconds = parseInt(tokenData.expiry, 10);

        if (isNaN(expirySeconds) || expirySeconds < nowSeconds) {
            clearStoredSessionData();
            redirectToLogin();
            return false;
        }
        displayUserInfo(tokenData.email, tokenData.role);
        return tokenData;
    }

    function showLoading() {
        if (loadingElement) loadingElement.style.display = "block";
        if (form) form.style.display = "none";
        if (errorElement) errorElement.style.display = "none";
        if (successElement) successElement.style.display = "none";
    }

    function showMessage(element, message, isError) {
        if (element) {
            element.textContent = message;
            element.style.display = "block";
            element.classList.remove(isError ? "success-message" : "error-message");
            element.classList.add(isError ? "error-message" : "success-message");
        }
        if (loadingElement) loadingElement.style.display = "none";
        if (form) form.style.display = "block";
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
        form.addEventListener("submit", async function (event) {
            event.preventDefault();
            hideMessages();

            const currentPassword = currentPasswordInput.value;
            const newPassword = newPasswordInput.value;
            const confirmNewPassword = confirmNewPasswordInput.value;

            if (!currentPassword || !newPassword || !confirmNewPassword) {
                showMessage(errorElement, "All password fields are required.", true);
                return;
            }
            if (newPassword !== confirmNewPassword) {
                showMessage(errorElement, "New passwords do not match.", true);
                return;
            }
            const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_]).{8,}$/;
            if (!passwordRegex.test(newPassword)) {
                showMessage(errorElement,"New password: Min 8 chars, upper, lower, digit, special.", true);
                return;
            }

            const changePasswordData = {
                currentPassword: currentPassword,
                newPassword: newPassword,
                confirmNewPassword: confirmNewPassword
            };

            const currentToken = getTokenData()?.token;
            if (!currentToken) {
                showMessage(errorElement,"Session expired. Please login again.", true);
                setTimeout(redirectToLogin, 2000);
                return;
            }

            submitPasswordChange(changePasswordData, currentToken);
        });
    }

    async function submitPasswordChange(payload, token) {
        showLoading();
        try {
            const response = await fetch("/rest/user/changePassword", {
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
                throw new Error(responseBody.error || `Password change failed (${response.status})`);
            }

            showMessage(successElement, responseBody.success || "Password changed successfully!", false);
            form.reset();
            setTimeout(() => {
                window.location.href = "/page/dashboard.html";
            }, 2000);

        } catch (error) {
            console.error("Error changing password:", error);
            showMessage(errorElement, error.message || "An unexpected error occurred.", true);
        }
    }
});