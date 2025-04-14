document.addEventListener("DOMContentLoaded", () => {
    const loggedOutOptions = document.getElementById("logged-out-options");
    const loggedInOptions = document.getElementById("logged-in-options");
    const loadingMessage = document.getElementById("loading-message");
    const logoutButton = document.getElementById("index-logout-button");
    const indexUserInfo = document.getElementById("index-user-info");

    function getTokenData() {
        const token = localStorage.getItem("token");
        const email = localStorage.getItem("userEmail");
        const expiry = localStorage.getItem("tokenExpiry");
        if (!token || !email || !expiry) {
            return null;
        }
        return { token, email, expiry };
    }

    function clearStoredSessionData() {
        localStorage.removeItem("token");
        localStorage.removeItem("userRole");
        localStorage.removeItem("userEmail");
        localStorage.removeItem("tokenExpiry");
    }

    function checkLoginStatus() {
        if (loadingMessage) loadingMessage.style.display = "block";
        if (loggedOutOptions) loggedOutOptions.style.display = "none";
        if (loggedInOptions) loggedInOptions.style.display = "none";
        if (indexUserInfo) indexUserInfo.style.display = "none";

        const tokenData = getTokenData();
        let isLoggedIn = false;

        if (tokenData) {
            const nowSeconds = Math.floor(Date.now() / 1000);
            const expirySeconds = parseInt(tokenData.expiry, 10);
            if (!isNaN(expirySeconds) && expirySeconds >= nowSeconds) {
                isLoggedIn = true;
            } else {
                clearStoredSessionData();
            }
        }

        setTimeout(() => {
            if (loadingMessage) loadingMessage.style.display = "none";

            if (isLoggedIn && tokenData) {
                if (loggedInOptions) loggedInOptions.style.display = "block";
                if (indexUserInfo) {
                    indexUserInfo.textContent = `Logged in as: ${tokenData.email}`;
                    indexUserInfo.style.display = "block";
                }
            } else {
                if (loggedOutOptions) loggedOutOptions.style.display = "block";
            }
        }, 150);
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
        checkLoginStatus();
    }

    if (logoutButton) {
        logoutButton.addEventListener("click", logout);
    }

    checkLoginStatus();
});