document.addEventListener("DOMContentLoaded", () => {
    const usersContainer = document.getElementById("users-list-container");
    const errorContainer = document.getElementById("list-users-error");
    const loadingContainer = document.getElementById("list-users-loading");
    const userInfoHeader = document.getElementById("user-info-header");
    const logoutButton = document.getElementById("fixed-logout-btn");

    const ROLE_ADMIN = "ADMIN";
    const NOT_DEFINED = "N/A";

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
        if (tokenData.role !== ROLE_ADMIN) {
            showError("Access Denied: Admin role required.");
            if(usersContainer) usersContainer.innerHTML = "";
            return null;
        }
        return tokenData;
    }

    function showLoading() {
        if(loadingContainer) loadingContainer.style.display = "block";
        if(errorContainer) errorContainer.style.display = "none";
        if(usersContainer) usersContainer.innerHTML = "";
    }

    function showError(message) {
        if (errorContainer) {
            errorContainer.textContent = message;
            errorContainer.style.display = "block";
        }
        if(loadingContainer) loadingContainer.style.display = "none";
    }

    function displayUsersTable(users) {
        if (!usersContainer) {
            console.error("Cannot find users-list-container element.");
            return;
        }
        if(loadingContainer) loadingContainer.style.display = "none";
        if(errorContainer) errorContainer.style.display = "none";

        if (!users || users.length === 0) {
            usersContainer.innerHTML = "<p>No users found.</p>";
            return;
        }

        const headers = [
            "Email", "Username", "Full Name", "Role", "State", "Profile",
            "Phone", "CC Number", "NIF", "Address", "Employer", "Job", "Employer NIF"
        ];
        const properties = [
            "email", "username", "fullName", "role", "state", "profile",
            "phone", "ccNumber", "nif", "address", "employer", "job", "employerNIF"
        ];


        let tableHTML = `
            <table>
                <thead>
                    <tr>`;
        headers.forEach(header => {
            tableHTML += `<th>${header}</th>`;
        });
        tableHTML += `
                    </tr>
                </thead>
                <tbody>
        `;

        users.forEach(user => {
            tableHTML += `<tr>`;
            properties.forEach(prop => {
                const value = user[prop];
                tableHTML += `<td>${(value !== null && value !== undefined && value !== "") ? value : NOT_DEFINED}</td>`;
            });
            tableHTML += `</tr>`;
        });

        tableHTML += `
                </tbody>
            </table>
        `;

        usersContainer.innerHTML = tableHTML;
    }

    async function fetchUserList(token) {
        showLoading();
        try {
            const response = await fetch("/rest/listUsers", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`
                }
            });

            const data = await response.json();

            if (!response.ok) {
                const errorMsg = data?.error || `HTTP error! Status: ${response.status}`;
                throw new Error(errorMsg);
            }

            if (data && data.success && Array.isArray(data.users)) {
                displayUsersTable(data.users);
            } else {
                throw new Error("Invalid data format received from server.");
            }

        } catch (error) {
            console.error("Error fetching or processing user list:", error);
            showError(`Failed to load users: ${error.message}`);
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

    const tokenData = checkAuth();
    if (tokenData) {
        fetchUserList(tokenData.token);
    }

    if (logoutButton) {
        logoutButton.addEventListener("click", logout);
    }

})();