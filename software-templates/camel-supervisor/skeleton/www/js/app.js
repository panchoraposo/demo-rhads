function apiUrl(path) {
    const base = window.APP_BASE || "";
    const p = path.charAt(0) === "/" ? path : "/" + path;
    return base + p;
}

const SHOW_CONDITION = document.body.dataset.showCondition !== "false";
const COLSPAN = SHOW_CONDITION ? 7 : 6;

let currentSortColumn = "id";
let currentSortDirection = "asc";
let carsData = [];
let currentFilterText = "";
let currentFilterField = "all";
let lastUpdatedCarId = null;

document.addEventListener("DOMContentLoaded", function () {
    loadAllCars();
    setupEventListeners();
    setupSorting();
    fetch(apiUrl("/llm"))
        .then((r) => r.json())
        .then((d) => {
            const el = document.getElementById("llm-label");
            if (el && d.label) {
                el.textContent = "· " + d.label;
            }
        })
        .catch(() => {});
});

function loadAllCars() {
    fetch(apiUrl("/cars"))
        .then((response) => {
            if (!response.ok) {
                throw new Error("Network response was not ok");
            }
            return response.json();
        })
        .then((cars) => {
            carsData = cars;
            sortCars();
            populateFleetStatusTable(carsData);
        })
        .catch((error) => {
            console.error("Error fetching cars:", error);
            displayError("Failed to load car data. Please try again later.");
        });
}

function setupSorting() {
    document.querySelectorAll(".sortable").forEach((header) => {
        header.addEventListener("click", function () {
            const column = this.getAttribute("data-sort");
            if (column === currentSortColumn) {
                currentSortDirection = currentSortDirection === "asc" ? "desc" : "asc";
            } else {
                currentSortColumn = column;
                currentSortDirection = "asc";
            }
            updateSortHeaders();
            sortCars();
            populateFleetStatusTable(carsData);
        });
    });
}

function updateSortHeaders() {
    document.querySelectorAll(".sortable").forEach((header) => {
        header.classList.remove("sort-asc", "sort-desc");
    });
    const currentHeader = document.querySelector(`.sortable[data-sort="${currentSortColumn}"]`);
    if (currentHeader) {
        currentHeader.classList.add(currentSortDirection === "asc" ? "sort-asc" : "sort-desc");
    }
}

function sortCars() {
    carsData.sort((a, b) => {
        let valueA;
        let valueB;
        if (currentSortColumn === "status") {
            valueA = getStatusDisplay(a.status);
            valueB = getStatusDisplay(b.status);
        } else {
            valueA = a[currentSortColumn];
            valueB = b[currentSortColumn];
        }
        if (currentSortColumn === "id" || currentSortColumn === "year") {
            valueA = Number(valueA) || 0;
            valueB = Number(valueB) || 0;
        }
        if (valueA < valueB) {
            return currentSortDirection === "asc" ? -1 : 1;
        }
        if (valueA > valueB) {
            return currentSortDirection === "asc" ? 1 : -1;
        }
        return 0;
    });
}

function filterCars() {
    if (!currentFilterText) {
        return carsData;
    }
    return carsData.filter((car) => {
        const filterText = currentFilterText.toLowerCase();
        if (currentFilterField !== "all") {
            let fieldValue = car[currentFilterField];
            if (currentFilterField === "status") {
                fieldValue = getStatusDisplay(fieldValue);
            }
            return String(fieldValue).toLowerCase().includes(filterText);
        }
        return (
            String(car.id).toLowerCase().includes(filterText) ||
            car.make.toLowerCase().includes(filterText) ||
            car.model.toLowerCase().includes(filterText) ||
            String(car.year).toLowerCase().includes(filterText) ||
            (car.condition && car.condition.toLowerCase().includes(filterText)) ||
            getStatusDisplay(car.status).toLowerCase().includes(filterText)
        );
    });
}

function populateFleetStatusTable(cars) {
    const tableBody = document.getElementById("fleet-status-table-body");
    tableBody.innerHTML = "";
    const filteredCars = currentFilterText ? filterCars() : cars;

    if (filteredCars.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="${COLSPAN}">No cars match your filter criteria</td></tr>`;
        return;
    }

    filteredCars.forEach((car) => {
        const row = document.createElement("tr");
        if (car.id === lastUpdatedCarId) {
            row.classList.add("highlight-row");
            setTimeout(() => {
                lastUpdatedCarId = null;
            }, 3000);
        }

        const statusPillClass = getStatusPillClass(car.status);
        let actionCell = "<td></td>";
        if (
            car.status === "RENTED" ||
            car.status === "AT_CLEANING" ||
            car.status === "AT_MAINTENANCE" ||
            car.status === "IN_MAINTENANCE"
        ) {
            actionCell = `
                <td>
                    <form data-car-id="${car.id}" data-car-status="${car.status}">
                        <input type="text" class="feedback-input" name="feedback" placeholder="Enter feedback">
                        <button type="submit" class="return-button">Return</button>
                    </form>
                </td>`;
        }

        const conditionCell = SHOW_CONDITION ? `<td>${car.condition || "N/A"}</td>` : "";
        row.innerHTML = `
            <td>${car.id}</td>
            <td>${car.make}</td>
            <td>${car.model}</td>
            <td>${car.year}</td>
            ${conditionCell}
            <td><span class="status-pill ${statusPillClass}">${getStatusDisplay(car.status)}</span></td>
            ${actionCell}
        `;
        tableBody.appendChild(row);
    });
}

function processFeedback(event) {
    event.preventDefault();
    const form = event.target;
    const carId = form.getAttribute("data-car-id");
    const status = form.getAttribute("data-car-status");
    const feedback = form.querySelector('[name="feedback"]').value;
    const button = form.querySelector("button");

    button.disabled = true;
    button.classList.add("loading");
    const originalText = button.textContent;
    button.textContent = "Processing...";

    const statusLabels = {
        RENTED: "rental",
        AT_CLEANING: "cleaning",
        AT_MAINTENANCE: "maintenance",
        IN_MAINTENANCE: "maintenance",
    };

    const params = new URLSearchParams({ feedback: feedback });
    fetch(apiUrl(`/car-management/return/${carId}?${params.toString()}`), { method: "POST" })
        .then(async (response) => {
            const raw = await response.text();
            let payload = raw;
            try {
                payload = raw ? JSON.parse(raw) : {};
            } catch (ignore) {
                payload = { message: raw };
            }
            if (!response.ok) {
                throw new Error(payload.error || payload.message || "Network response was not ok");
            }
            return payload;
        })
        .then(() => {
            lastUpdatedCarId = Number(carId);
            showNotification(`Car successfully returned from ${statusLabels[status] || status}`);
            loadAllCars();
        })
        .catch((error) => {
            console.error(`Error returning car from ${statusLabels[status] || status}:`, error);
            displayError(
                error.message ||
                    `Failed to process ${statusLabels[status] || status} return. Please try again.`
            );
            button.disabled = false;
            button.classList.remove("loading");
            button.textContent = originalText;
        });
}

function getStatusPillClass(status) {
    switch (status) {
        case "RENTED":
            return "status-pill-rented";
        case "AT_CLEANING":
            return "status-pill-cleaning";
        case "AT_MAINTENANCE":
        case "IN_MAINTENANCE":
            return "status-pill-maintenance";
        case "PENDING_DISPOSITION":
            return "status-pill-disposition";
        case "AVAILABLE":
            return "status-pill-available";
        default:
            return "";
    }
}

function getStatusDisplay(status) {
    switch (status) {
        case "RENTED":
            return "Rented";
        case "AT_CLEANING":
            return "At Cleaning";
        case "AT_MAINTENANCE":
        case "IN_MAINTENANCE":
            return "In Maintenance";
        case "PENDING_DISPOSITION":
            return "Pending Disposition";
        case "AVAILABLE":
            return "Available to Rent";
        default:
            return status;
    }
}

function setupEventListeners() {
    const refreshButton = document.getElementById("refresh-button");
    if (refreshButton) {
        refreshButton.addEventListener("click", loadAllCars);
    }

    const filterInput = document.getElementById("fleet-filter");
    if (filterInput) {
        filterInput.addEventListener("input", function () {
            currentFilterText = this.value;
            populateFleetStatusTable(carsData);
        });
    }

    const filterField = document.getElementById("filter-field");
    if (filterField) {
        filterField.addEventListener("change", function () {
            currentFilterField = this.value;
            populateFleetStatusTable(carsData);
        });
    }

    const clearFilterButton = document.getElementById("clear-filter");
    if (clearFilterButton) {
        clearFilterButton.addEventListener("click", function () {
            const filterInput = document.getElementById("fleet-filter");
            const filterField = document.getElementById("filter-field");
            currentFilterText = "";
            currentFilterField = "all";
            if (filterInput) filterInput.value = "";
            if (filterField) filterField.value = "all";
            populateFleetStatusTable(carsData);
        });
    }

    const tableBody = document.getElementById("fleet-status-table-body");
    if (tableBody) {
        tableBody.addEventListener("submit", function (event) {
            if (event.target.matches("form[data-car-id]")) {
                processFeedback(event);
            }
        });
    }
}

function displayError(message) {
    const errorDiv = document.getElementById("error-message");
    if (errorDiv) {
        errorDiv.textContent = message;
        errorDiv.style.display = "block";
        setTimeout(() => {
            errorDiv.style.display = "none";
        }, 5000);
    } else {
        alert(message);
    }
}

function showNotification(message) {
    const notificationDiv = document.getElementById("notification");
    if (notificationDiv) {
        notificationDiv.textContent = message;
        notificationDiv.style.display = "block";
        setTimeout(() => {
            notificationDiv.style.display = "none";
        }, 3000);
    }
}
