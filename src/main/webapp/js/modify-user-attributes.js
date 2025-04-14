document.addEventListener("DOMContentLoaded", () => {
    const modifyForm = document.getElementById("modify-attributes-form");
    const targetEmailInput = document.getElementById("target-email");

    const loadingElement = document.getElementById("modify-user-loading");
    const errorElement = document.getElementById("error-message-mua");
    const successElement = document.getElementById("success-message-mua");
    const userInfoElement = document.getElementById("user-info-header");
    const logoutButton = document.getElementById("fixed-logout-btn");

    const photoInput = document.getElementById("photo");
    const photoPreviewImg = document.getElementById("photo-preview-img");
    const photoPreviewPlaceholder = document.getElementById("photo-preview-placeholder");

    const usernameInput = document.getElementById("username");
    const roleInput = document.getElementById("role");
    const stateInput = document.getElementById("state");

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

    function checkAuthAndSetupForm() {
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
            hideForms();
            return null;
        }

        setFieldPermissions();
        return tokenData;
    }

    function setFieldPermissions() {
        const isAdmin = currentUserRole === ROLE_ADMIN;

        if (usernameInput) usernameInput.readOnly = !isAdmin;
        if (roleInput) {
            roleInput.disabled = false;
            Array.from(roleInput.options).forEach(opt => {
                opt.disabled = !isAdmin && (opt.value === ROLE_ADMIN || opt.value === ROLE_BACKOFFICE);
            });
        }
        if (stateInput) {
            stateInput.disabled = false;
            Array.from(stateInput.options).forEach(opt => {
                opt.disabled = !isAdmin && opt.value === "SUSPENDED";
            });
        }
    }


    function showLoading() {
        if (loadingElement) loadingElement.style.display = "block";
        if (modifyForm) modifyForm.style.pointerEvents = "none";
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
        if (modifyForm) modifyForm.style.pointerEvents = "auto";
    }

    function hideMessages() {
        if (errorElement) errorElement.style.display = "none";
        if (successElement) successElement.style.display = "none";
    }

    function hideForms() {
        if (modifyForm) modifyForm.style.display = "none";
        if (loadingElement) loadingElement.style.display = "none";
    }

    if (photoInput && photoPreviewImg) {
        photoInput.addEventListener("change", function (event) {
            const file = event.target.files[0];
            if (file && file.type.startsWith("image/")) {
                const reader = new FileReader();
                reader.onload = function (e) {
                    photoPreviewImg.src = e.target.result;
                    photoPreviewImg.style.display = "block";
                    if (photoPreviewPlaceholder) photoPreviewPlaceholder.style.display = "none";
                }
                reader.readAsDataURL(file);
            } else {
                photoPreviewImg.src = "#";
                photoPreviewImg.style.display = "none";
                if (photoPreviewPlaceholder) photoPreviewPlaceholder.style.display = "block";
            }
        });
    }

    if (modifyForm) {
        modifyForm.addEventListener("submit", function (event) {
            event.preventDefault();
            hideMessages();

            const targetEmail = targetEmailInput.value.trim();
            if (!targetEmail) {
                showMessage(errorElement,"Target User Email is required.", true);
                return;
            }

            const updateData = {
                email: targetEmail,
                username: document.getElementById("username").value.trim() || null,
                fullName: document.getElementById("fullName").value.trim() || null,
                phone: document.getElementById("phone").value.trim() || null,
                profile: document.getElementById("profile").value || null,
                role: document.getElementById("role").value || null,
                state: document.getElementById("state").value || null,
                ccNumber: document.getElementById("ccNumber").value.trim() || null,
                nif: document.getElementById("nif").value.trim() || null,
                employer: document.getElementById("employer").value.trim() || null,
                job: document.getElementById("job").value.trim() || null,
                address: document.getElementById("address").value.trim() || null,
                employerNIF: document.getElementById("employerNIF").value.trim() || null,
                photo: null
            };

            const file = photoInput.files[0];
            if (file && file.type.startsWith("image/")) {
                if (file.size > 2 * 1024 * 1024) {
                    showMessage(errorElement,"Photo file size exceeds limit (e.g., 2MB).", true);
                    return;
                }
                const reader = new FileReader();
                reader.onloadend = function () {
                    if (reader.result) {
                        updateData.photo = reader.result.split(",")[1];
                        submitAttributeUpdate(updateData);
                    } else {
                        showMessage(errorElement,"Error processing photo file.", true);
                    }
                };
                reader.onerror = function () {
                    showMessage(errorElement,"Error reading photo file.", true);
                };
                reader.readAsDataURL(file);
            } else {
                submitAttributeUpdate(updateData);
            }
        });
    }

    async function submitAttributeUpdate(dataToSend) {
        const tokenData = checkAuthAndSetupForm();
        if (!tokenData) return;
        showLoading();

        try {
            const response = await fetch("/rest/account/changeAttributes", {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${tokenData.token}`,
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(dataToSend)
            });

            let responseBody;
            try { responseBody = await response.json(); }
            catch (e) { responseBody = { error: `Update failed. Server returned non-JSON response (Status: ${response.status})` }; }

            if (!response.ok || !responseBody.success) {
                throw new Error(responseBody.error || `Update failed (${response.status})`);
            }
            showMessage(successElement, responseBody.success || "User attributes updated successfully!", false);
            modifyForm.reset();
            if (photoPreviewImg) photoPreviewImg.style.display = 'none';
            if (photoPreviewPlaceholder) photoPreviewPlaceholder.style.display = 'block';


        } catch (error) {
            console.error("Error updating attributes:", error);
            showMessage(errorElement, error.message || "An error occurred during update.", true);
        } finally {
            if (loadingElement) loadingElement.style.display = "none";
            if (modifyForm) modifyForm.style.pointerEvents = "auto";
        }
    }

    async function logout() {
        const tokenData = getTokenData();
        if (tokenData?.token) {
            try { await fetch("/rest/logout", { method: "POST", headers: {"Authorization": `Bearer ${tokenData.token}`} }); }
            catch (error) { console.error("Logout fetch failed:", error); }
        }
        clearStoredSessionData();
        redirectToLogin();
    }

    const initialTokenData = checkAuthAndSetupForm();

    if (initialTokenData) {
        if (modifyForm) modifyForm.style.display = "block";
    }

    if (logoutButton) {
        logoutButton.addEventListener("click", logout);
    }

});