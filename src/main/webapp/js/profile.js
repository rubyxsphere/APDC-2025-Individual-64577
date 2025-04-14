document.addEventListener("DOMContentLoaded", () => {
    const loadingElement = document.getElementById("profile-loading");
    const errorElement = document.getElementById("profile-error");
    const contentElement = document.getElementById("profile-content");
    const photoElement = document.getElementById("profile-photo");
    const noPhotoPlaceholder = document.getElementById("no-photo-placeholder");
    const photoContainer = document.getElementById("profile-photo-container");
    const userInfoHeader = document.getElementById("user-info-header");

    const profileFields = {
        "profile-email": "email",
        "profile-username": "username",
        "profile-fullName": "fullName",
        "profile-phone": "phone",
        "profile-profile": "profile",
        "profile-role": "role",
        "profile-state": "state",
        "profile-ccNumber": "ccNumber",
        "profile-nif": "nif",
        "profile-employer": "employer",
        "profile-job": "job",
        "profile-address": "address",
        "profile-employerNIF": "employerNIF"
    };

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
        if (errorElement) errorElement.style.display = "none";
        if (contentElement) contentElement.style.display = "none";
    }

    function showError(message) {
        if (errorElement) {
            errorElement.textContent = message || "Error loading profile.";
            errorElement.style.display = "block";
        }
        if (loadingElement) loadingElement.style.display = "none";
        if (contentElement) contentElement.style.display = "none";
    }

    function displayProfile(data) {
        if (!data) return;

        for (const id in profileFields) {
            const element = document.getElementById(id);
            const dataKey = profileFields[id];
            if (element) {
                const value = data[dataKey];
                element.textContent = (value !== null && value !== undefined && value !== "") ? value : "NOT DEFINED";
            }
        }

        if (data.photo && photoElement && photoContainer) {
            const mimeType = "image/jpeg";
            photoElement.src = `data:${mimeType};base64,${data.photo}`;
            photoElement.style.display = "block";
            if (noPhotoPlaceholder) noPhotoPlaceholder.style.display = "none";
            photoContainer.style.border = "none";
            photoContainer.style.backgroundColor = "transparent";
        } else {
            if (photoElement) photoElement.style.display = "none";
            if (noPhotoPlaceholder) noPhotoPlaceholder.style.display = "flex";
            if (photoContainer) {
                photoContainer.style.border = "2px dashed #ccc";
                photoContainer.style.backgroundColor = "#f0f0f0";
            }
        }

        if (contentElement) contentElement.style.display = "block";
        if (loadingElement) loadingElement.style.display = "none";
        if (errorElement) errorElement.style.display = "none";
    }


    async function fetchProfileData(token) {
        showLoading();
        try {
            const response = await fetch("/rest/profile/me", {
                method: "GET",
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "Accept": "application/json"
                }
            });

            if (!response.ok) {
                if (response.status === 401 || response.status === 403) {
                    clearStoredSessionData();
                    redirectToLogin();
                    return;
                }
                let errorMsg = `Failed to fetch profile (${response.status})`;
                try {
                    const errData = await response.json();
                    errorMsg = errData.error || errorMsg;
                } catch (e) {}
                throw new Error(errorMsg);
            }
            const data = await response.json();
            displayProfile(data);
        } catch (error) {
            console.error("Error fetching profile:", error);
            showError(error.message);
        }
    }

    const tokenData = checkAuth();
    if (tokenData) {
        fetchProfileData(tokenData.token);
    }

});