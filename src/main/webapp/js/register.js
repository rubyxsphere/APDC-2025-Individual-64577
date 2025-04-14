document.addEventListener("DOMContentLoaded", () => {
    const registrationForm = document.getElementById("registration-form");
    const errorMessageElement = document.getElementById("error-message-reg");
    const successMessageElement = document.getElementById("success-message-reg");

    function showMessage(element, message, isError) {
        if (element) {
            element.textContent = message;
            element.style.display = "block";
            if (isError) {
                element.classList.remove("success-message");
                element.classList.add("error-message");
            } else {
                element.classList.remove("error-message");
                element.classList.add("success-message");
            }
        }
    }

    function hideMessages() {
        if (errorMessageElement) errorMessageElement.style.display = "none";
        if (successMessageElement) successMessageElement.style.display = "none";
    }

    if (registrationForm) {
        registrationForm.addEventListener("submit", function (event) {
            event.preventDefault();
            hideMessages();

            const password = document.getElementById("password").value;
            const confirmPassword = document.getElementById("confirmPassword").value;

            if (password !== confirmPassword) {
                showMessage(errorMessageElement, "Passwords do not match.", true);
                return;
            }

            const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_]).{8,}$/;
            if (!passwordRegex.test(password)) {
                showMessage(errorMessageElement, "Password: Min 8 chars, upper, lower, digit, special char.", true);
                return;
            }

            const formData = {
                email: document.getElementById("email").value.trim(),
                username: document.getElementById("username").value.trim(),
                fullName: document.getElementById("fullName").value.trim(),
                phone: document.getElementById("phone").value.trim(),
                password: password,
                confirmPassword: confirmPassword,
                profile: document.getElementById("profile").value,
                ccNumber: document.getElementById("ccNumber").value.trim() || null,
                nif: document.getElementById("nif").value.trim() || null,
                employer: document.getElementById("employer").value.trim() || null,
                job: document.getElementById("job").value.trim() || null,
                address: document.getElementById("address").value.trim() || null,
                employerNIF: document.getElementById("employerNIF").value.trim() || null,
                photo: null
            };

            const requiredFields = ["email", "username", "fullName", "phone", "password", "profile"];
            for (const field of requiredFields) {
                if (!formData[field]) {
                    showMessage(errorMessageElement, `Required field '${field}' is missing.`, true);
                    return;
                }
            }

            const photoInput = document.getElementById("photo");
            const photoFile = photoInput.files[0];

            if (photoFile) {
                if (!photoFile.type.startsWith("image/")) {
                    showMessage(errorMessageElement,"Please select a valid image file for photo.", true);
                    return;
                }
                if (photoFile.size > 2 * 1024 * 1024) { // Example size limit: 2MB
                    showMessage(errorMessageElement,"Photo file size exceeds limit (e.g., 2MB).", true);
                    return;
                }

                const reader = new FileReader();
                reader.onloadend = function () {
                    if (reader.result) {
                        const base64String = reader.result.split(",")[1];
                        formData.photo = base64String;
                        sendData(formData);
                    } else {
                        showMessage(errorMessageElement,"Error processing photo file.", true);
                    }
                };
                reader.onerror = function () {
                    console.error("FileReader error:", reader.error);
                    showMessage(errorMessageElement,"Error reading photo file.", true);
                }
                reader.readAsDataURL(photoFile);
            } else {
                sendData(formData);
            }
        });
    }

    async function sendData(data) {
        try {
            const response = await fetch("/rest/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(data)
            });

            let responseBody;
            try {
                responseBody = await response.json();
            } catch (e) {
                responseBody = { error: `Registration failed. Server returned non-JSON response (Status: ${response.status})` };
            }


            if (!response.ok || !responseBody.success) {
                throw new Error(responseBody.error || `Registration failed (Status: ${response.status})`);
            }

            showMessage(successMessageElement, responseBody.success || "User registered successfully!", false);
            registrationForm.reset();
            setTimeout(() => {
                window.location.href = "/page/login.html";
            }, 2500);

        } catch (error) {
            console.error("Registration error:", error);
            showMessage(errorMessageElement, error.message || "An error occurred during registration.", true);
        }
    }
});