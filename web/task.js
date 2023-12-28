/* 
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */


/**
 * Creates a new empty task item and displays it in the form area.
 * @returns {undefined}
 */
function addTask() {
    var task = {};
    task.lid = "-1";
    fillForm(task);
}

/**
 * Displays the given task item in the form area.
 * @param {type} taskData
 * @returns {undefined}
 */
function fillForm(taskData) {
    hideDialog();
    selectedTask = taskData;
    var btAdv = document.getElementById("advance");
    if (btAdv) {
        btAdv.disabled = taskData.lId === "-1";
    }
    
    var status = document.getElementById("status");
    status.innerText = "";

    setValue("tasklid", taskData.lId);
    setValue("name", taskData.name);
    setValue("description", taskData.description);
    setValue("owner", userlist.nameFromId(taskData.owner));
    setValue("duedate", taskData.nextDueDate);
    setValue("lastchanged", formatIso(taskData.lastChanged));
    fillInterval(taskData.interval);

    var btSave = document.getElementById("save");
    if (btSave) {
        btSave.disabled = true;
    }
}

/**
 * Splits the given intervalData string into parts
 * and fill the interval fields with the given values.
 * 
 * @param {type} intervalData
 * @returns {undefined}
 */
function fillInterval(intervalData) {
    var intervalInitNeeded = true;
    if (intervalData) {
        var intervalParts = intervalData.split("|");
        if (intervalParts.length === 3) {
            document.getElementById(intervalParts[0]).checked = true;
            setValue("divider", intervalParts[1]);
            var startDates = intervalParts[2].split(";");
            document.getElementById("datelist").innerHTML = "";
            startDates.forEach((startDate) => {addDateField(startDate);});
            intervalInitNeeded = false;
        }
    }

    if (intervalInitNeeded) {
        document.getElementById("single").checked = true;
        document.getElementById("divider").value = 1;
        document.getElementById("datelist").innerHTML = "";
        addDateField();
    }
}

/**
 * Populates the tasklist with the selection of the
 * returned task items.
 * @param {type} tasklist
 * @returns {undefined}
 */
function listTasks(tasklist) {
    var list = document.getElementById("tasklist");
    list.innerHTML = "";
    var td = toDay();
    tasklist.forEach((task) => {
        var newItem = document.createElement("div");
        newItem.className = "listitem";
        var taskName = document.createElement("div");
        taskName.className = "taskname";
        taskName.innerText = task.name;
        var dueDate = document.createElement("div");
        dueDate.innerText = task.nextDueDate;
        dueDate.className = "duedate";
        if (task.nextDueDate < td) {
            dueDate.classList.add("escalated");
        }

        newItem.appendChild(taskName);
        newItem.appendChild(dueDate);
        newItem.userData = task;
        newItem.onclick = function() {fillForm(task);};
        list.appendChild(newItem);
    });
}

/**
 * Adds a date field into the list of start dates.
 * @param {type} initialValue
 * @returns {undefined}
 */
function addDateField(initialValue) {
    var root = document.getElementById("datelist");
    var dateField = document.createElement("input");
    dateField.type = "date";
    dateField.style = "margin-right: 5pt;";
    dateField.name = "startdate";
    dateField.onchange = function() {changed();};
    if (initialValue) {
        dateField.value = initialValue;
    }
    root.appendChild(dateField);
}

/**
 * Reads the values of the interval fields and build
 * the interval string for storage.
 * @returns {String}
 */
function getInterval() {
    var interval = document.querySelector('input[name="interval"]:checked').value;
    var divider = document.getElementById("divider").value;
    var dates = getValues("startdate", ";");
    return interval + "|" + divider + "|" + dates;
}
