document.addEventListener("DOMContentLoaded", () => {
    const loginForm = document.getElementById("login-form");
    const errorMessageElement = document.getElementById("error-message");
    const emailInput = document.getElementById("email");
    const passwordInput = document.getElementById("password");

    function showErrorMessage(message) {
        if (errorMessageElement) {
            errorMessageElement.textContent = message;
            errorMessageElement.style.display = "block";
        }
    }

    function hideErrorMessage() {
        if (errorMessageElement) {
            errorMessageElement.textContent = "";
            errorMessageElement.style.display = "none";
        }
    }

    if (loginForm) {
        loginForm.addEventListener("submit", async function (event) {
            event.preventDefault();
            hideErrorMessage();

            const email = emailInput.value.trim();
            const password = passwordInput.value.trim();

            if (!email || !password) {
                showErrorMessage("Both email and password are required.");
                return;
            }

            try {
                const response = await fetch("/rest/login", {
                    method: "POST",
                    headers: {"Content-Type": "application/json"},
                    body: JSON.stringify({email: email, password: password})
                });

                const data = await response.json();

                if (!response.ok || !data.success) {
                    throw new Error(data.error || `Login failed (Status: ${response.status})`);
                }

                if (data.token && data.token.token && data.token.role && data.token.email && data.token.validTo) {
                    localStorage.setItem("token", data.token.token);
                    localStorage.setItem("userRole", data.token.role);
                    localStorage.setItem("userEmail", data.token.email);
                    localStorage.setItem("tokenExpiry", data.token.validTo);
                    window.location.href = "/page/dashboard.html";
                } else {
                    throw new Error("Received invalid token data from server.");
                }
            } catch (err) {
                console.error("Login error:", err);
                showErrorMessage(err.message || "An unexpected error occurred.");
            }
        });
    }
});