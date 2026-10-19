
(function () {
    "use strict";

    // API endpoints
    // These paths assume the frontend and Java backend
    // are served from the same web application.
    const SCAN_ENDPOINT = "scan";
    const HISTORY_ENDPOINT = "history";

    // HTML elements
    const form = document.getElementById("scan-form");
    const urlInput = document.getElementById("url-input");

    if (!form || !urlInput) {
        console.error(
            "Required HTML elements not found. Check scan-form and url-input."
        );
        return;
    }

    const submitBtn = form.querySelector("button[type='submit']");
    const resultCard = document.getElementById("result-card");
    const resultUrl = document.getElementById("result-url");
    const scoreTrack = document.getElementById("score-track");
    const scoreBar = document.getElementById("score-bar");
    const resultMessage = document.getElementById("result-message");
    const historyBody = document.getElementById("history-body");

    // Create a status badge if the HTML does not contain one.
    let resultStatus = document.getElementById("result-status");

    if (!resultStatus && resultMessage) {
        resultStatus = document.createElement("span");
        resultStatus.id = "result-status";
        resultMessage.parentNode.insertBefore(
            resultStatus,
            resultMessage
        );
    }

    console.log("Phishing Detector is ready!");

    // Keep scores between 0 and 100.
    function clampScore(value) {
        const number = Number(value);

        if (!Number.isFinite(number)) {
            return null;
        }

        return Math.max(0, Math.min(100, number));
    }

    // Assign a badge class based on the backend's status.
    function statusClass(status) {
        const value = String(status || "").toLowerCase();

        if (/suspicious|phishing|malicious|high risk/.test(value)) {
            return "status-suspicious";
        }

        if (/safe|legitimate|low risk|no major warning/.test(value)) {
            return "status-safe";
        }

        return "status-unknown";
    }

    // Display a readable date and time.
    function formatTime(value) {
        if (!value) {
            return "-";
        }

        const date = new Date(value);

        return Number.isNaN(date.getTime())
            ? String(value)
            : date.toLocaleString();
    }

    // Read and parse a server response safely.
    async function readJsonResponse(response) {
        const text = await response.text();

        let data = null;

        if (text.trim() !== "") {
            try {
                data = JSON.parse(text);
            } catch (error) {
                throw new Error(
                    "The server returned invalid JSON. Check the backend endpoint."
                );
            }
        }

        if (!response.ok) {
            const message =
                data && (data.error || data.message);

            throw new Error(
                message || "Request failed with HTTP " + response.status
            );
        }

        if (data === null) {
            throw new Error(
                "The server returned an empty response."
            );
        }

        return data;
    }

    // Display a successful scan result.
    function showResult(data, scannedUrl) {
        const rawScore = data.riskScore ?? data.score;
        const score = clampScore(rawScore);

        if (score === null || !data.status) {
            throw new Error(
                "The server response is missing a valid riskScore or status."
            );
        }

        const status = String(data.status);
        const cssClass = statusClass(status);

        resultCard.hidden = false;
        resultUrl.textContent = data.url || scannedUrl;

        scoreBar.style.width = score + "%";
        scoreBar.className = cssClass;

        scoreTrack.setAttribute(
            "aria-valuenow",
            String(score)
        );

        resultStatus.textContent = status;
        resultStatus.className = "status-badge " + cssClass;
        resultStatus.hidden = false;

        resultMessage.textContent =
            "Risk score: " + score + " / 100";
    }

    // Display errors without breaking the page.
    function showError(message, scannedUrl) {
        resultCard.hidden = false;
        resultUrl.textContent = scannedUrl || "";

        scoreBar.style.width = "0%";
        scoreBar.className = "";

        scoreTrack.setAttribute("aria-valuenow", "0");

        resultStatus.textContent = "";
        resultStatus.className = "";
        resultStatus.hidden = true;

        resultMessage.textContent = message;
    }

    // Send a URL to the Java ScanServlet.
    async function scanUrl(url) {
        const response = await fetch(SCAN_ENDPOINT, {
            method: "POST",
            headers: {
                "Content-Type":
                    "application/x-www-form-urlencoded"
            },
            body: new URLSearchParams({
                url: url
            })
        });

        return await readJsonResponse(response);
    }

    // Load recent scans from the Java history endpoint.
    async function loadHistory() {
        if (!historyBody) {
            console.warn(
                "History table body not found. Expected id='history-body'."
            );
            return;
        }

        try {
            const response = await fetch(HISTORY_ENDPOINT);
            const data = await readJsonResponse(response);

            // Support either a JSON array or an object containing items.
            const items = Array.isArray(data)
                ? data
                : data.items || data.history;

            if (!Array.isArray(items)) {
                throw new Error(
                    "History response must contain a JSON array."
                );
            }

            historyBody.replaceChildren();

            if (items.length === 0) {
                const row = historyBody.insertRow();
                const cell = row.insertCell();

                cell.colSpan = 4;
                cell.textContent = "No scans yet.";
                return;
            }

            items.forEach(function (item) {
                const row = historyBody.insertRow();
                const status = String(item.status || "");
                const score = clampScore(
                    item.riskScore ?? item.score
                );

                row.insertCell().textContent =
                    item.url || item.websiteUrl || "-";

                row.insertCell().textContent =
                    score === null ? "-" : score + " / 100";

                const statusCell = row.insertCell();
                const badge = document.createElement("span");

                badge.textContent = status || "-";
                badge.className =
                    "status-badge " + statusClass(status);

                statusCell.appendChild(badge);

                row.insertCell().textContent = formatTime(
                    item.scanTime ||
                    item.scannedAt ||
                    item.timestamp
                );
            });
        } catch (error) {
            console.error("History loading failed:", error);

            historyBody.replaceChildren();

            const row = historyBody.insertRow();
            const cell = row.insertCell();

            cell.colSpan = 4;
            cell.textContent =
                "Could not load scan history. Check the backend.";
        }
    }

    // Handle form submission.
    form.addEventListener("submit", async function (event) {
        event.preventDefault();

        const url = urlInput.value.trim();

        if (!url) {
            showError("Please enter a URL to scan.", "");
            urlInput.focus();
            return;
        }

        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.textContent = "Scanning...";
        }

        resultCard.hidden = false;
        resultUrl.textContent = url;
        resultMessage.textContent = "Scanning URL, please wait...";
        resultStatus.hidden = true;
        scoreBar.style.width = "0%";
        scoreTrack.setAttribute("aria-valuenow", "0");

        try {
            const data = await scanUrl(url);

            showResult(data, url);

            // Refresh the history after a successful scan.
            await loadHistory();
        } catch (error) {
            console.error("Scan failed:", error);

            let message = error.message;

            if (error instanceof TypeError) {
                message =
                    "Could not reach the server. Check the backend and API URL.";
            }

            showError(message, url);
        } finally {
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.textContent = "Scan URL";
            }
        }
    });

    // Load previous scans when the page opens.
    loadHistory();
})();
