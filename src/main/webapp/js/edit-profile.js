document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("edit-profile-form");
    const loadingElement = document.getElementById("edit-profile-loading");
    const errorElement = document.getElementById("error-message-edit");
    const successElement = document.getElementById("success-message-edit");
    const photoInput = document.getElementById("photo");
    const photoPreviewImg = document.getElementById("photo-preview-img");
    const photoPreviewPlaceholder = document.getElementById("photo-preview-placeholder");
    const userInfoElement = document.getElementById("user-info-header");
    const logoutButton = document.getElementById("fixed-logout-btn");

    const emailInput = document.getElementById("email");
    const usernameInput = document.getElementById("username");
    const roleInput = document.getElementById("role");
    const stateInput = document.getElementById("state");

    let currentPhotoBase64 = null;
    let currentUserRole = null;

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
        currentUserRole = tokenData.role;
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

    function populateForm(data) {
        const setText = (id, value) => {
            const el = document.getElementById(id);
            if (el) el.value = (value !== null && value !== undefined) ? value : "";
        };
        const setSelect = (id, value) => {
            const el = document.getElementById(id);
            if (el && value !== null && value !== undefined) el.value = value;
        };

        setText("email", data.email);
        setText("username", data.username);
        setText("fullName", data.fullName);
        setText("phone", data.phone);
        setSelect("profile", data.profile);
        setSelect("role", data.role);
        setSelect("state", data.state);
        setText("ccNumber", data.ccNumber);
        setText("nif", data.nif);
        setText("employer", data.employer);
        setText("job", data.job);
        setText("address", data.address);
        setText("employerNIF", data.employerNIF);

        currentPhotoBase64 = data.photo;
        if (data.photo && photoPreviewImg) {
            const mimeType = "image/jpeg";
            photoPreviewImg.src = `data:${mimeType};base64,${data.photo}`;
            photoPreviewImg.style.display = "block";
            if (photoPreviewPlaceholder) photoPreviewPlaceholder.style.display = "none";
        } else {
            if (photoPreviewImg) photoPreviewImg.style.display = "none";
            if (photoPreviewPlaceholder) photoPreviewPlaceholder.style.display = "block";
        }

        if (currentUserRole !== "ADMIN") {
            if (usernameInput) usernameInput.readOnly = true;
        } else {
            if (usernameInput) usernameInput.readOnly = false;
        }

        if (roleInput) roleInput.disabled = true;
        if (stateInput) stateInput.disabled = true;

        if (form) form.style.display = "block";
        if (loadingElement) loadingElement.style.display = "none";
    }

    async function loadProfileData(token) {
        showLoading();
        try {
            const response = await fetch("/rest/profile/me", {
                method: "GET",
                headers: {"Authorization": `Bearer ${token}`, "Accept": "application/json"}
            });
            if (!response.ok) {
                if (response.status === 401 || response.status === 403) {
                    clearStoredSessionData();
                    redirectToLogin();
                    return;
                }
                let errorMsg = `Failed to load profile (${response.status})`;
                try {
                    const errData = await response.json();
                    errorMsg = errData.error || errorMsg;
                } catch (e) {}
                throw new Error(errorMsg);
            }
            const data = await response.json();
            populateForm(data);

        } catch (error) {
            console.error("Error loading profile:", error);
            showMessage(errorElement, error.message, true);
        }
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

                if (currentPhotoBase64) {
                    const mimeType = "image/jpeg";
                    photoPreviewImg.src = `data:${mimeType};base64,${currentPhotoBase64}`;
                    photoPreviewImg.style.display = "block";
                    if (photoPreviewPlaceholder) photoPreviewPlaceholder.style.display = "none";
                } else {
                    photoPreviewImg.src = "#";
                    photoPreviewImg.style.display = "none";
                    if (photoPreviewPlaceholder) photoPreviewPlaceholder.style.display = "block";
                }
            }
        });
    }

    if (form) {
        form.addEventListener("submit", function (event) {
            event.preventDefault();
            hideMessages();

            const updateData = {
                email: document.getElementById("email").value.trim(),
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
                        submitUpdate(updateData);
                    } else {
                        showMessage(errorElement,"Error processing new photo.", true);
                    }
                };
                reader.onerror = function () {
                    showMessage(errorElement,"Error reading new photo file.", true);
                };
                reader.readAsDataURL(file);
            } else {
                submitUpdate(updateData);
            }
        });
    }

    async function submitUpdate(dataToSend) {
        const tokenData = checkAuth();
        if (!tokenData) return;

        showLoading();

        try {
            const response = await fetch("/rest/profile/me", {
                method: "PUT",
                headers: {
                    "Authorization": `Bearer ${tokenData.token}`,
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(dataToSend)
            });

            let responseBody;
            try {
                responseBody = await response.json();
            } catch (e) {
                responseBody = { error: `Update failed. Server returned non-JSON response (Status: ${response.status})` };
            }


            if (!response.ok || !responseBody.success) {
                throw new Error(responseBody.error || `Update failed (${response.status})`);
            }
            showMessage(successElement, responseBody.success || "Profile updated successfully!", false);
            photoInput.value = '';
            setTimeout(() => { loadProfileData(tokenData.token) }, 1500);
        } catch (error) {
            console.error("Error updating profile:", error);
            showMessage(errorElement, error.message || "An error occurred during update.", true);
        }
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
    if (initialTokenData) {
        loadProfileData(initialTokenData.token);
    }

    if (logoutButton) {
        logoutButton.addEventListener("click", logout);
    }

});