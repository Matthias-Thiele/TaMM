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
    task.lId = "-1";
    fillForm(task);
}

/**
 * Disables given button if there is no valid task.
 * 
 * @param {type} buttonName
 * @param {type} taskId
 * @returns {undefined}
 */
function disableElement(buttonName, taskId) {
    var bt = document.getElementById(buttonName);
    if (bt) {
        bt.disabled = (taskId === "-1") || !taskId;
    }
}

/**
 * Displays the given task item in the form area.
 * @param {type} taskData
 * @returns {undefined}
 */
function fillForm(taskData) {
    hideDialog();
    selectedTask = taskData;
    disableElement("advance", taskData.lId);
    disableElement("remove", taskData.lId);
    disableElement("history", taskData.lId);
    disableElement("duedate", taskData.lId);
    
    var status = document.getElementById("status");
    status.innerText = "";

    setValue("tasklid", taskData.lId);
    setValue("name", taskData.name);
    setValue("description", taskData.description);
    setValue("owner", userlist.nameFromId(taskData.owner));
    setValue("duedate", taskData.nextDueDate);
    setValue("createdate", formatIso(taskData.createDate));
    var changed = (taskData.createDate === taskData.lastChanged) ? "" : taskData.lastChanged
    setValue("lastchanged", formatIso(changed));
    fillInterval(taskData.interval);

    var btSave = document.getElementById("save");
    if (btSave) {
        btSave.disabled = true;
    }
    
    loadAttachments();
    document.getElementById("name").focus();
}

/**
 * Create a new attachment download link element.
 * 
 * @param {type} a attachment data.
 * @returns {HTMLElement|makeAttachmentLink.anchor}
 */
function makeAttachmentLink(a) {
    var div = document.createElement("div");
    var anchor = document.createElement("a");
    anchor.style = "text-decoration: none; color: black; border 1px solid blue";
    var text = document.createElement("span");
    text.innerText = a.fileName;
    text.style = "width: calc(100% - 25px); display:inline-block";
    var url = (a.url) ? a.url : "upload/" + a.guid + "/" + a.fileName;
    anchor.href = url;
    anchor.target = "_blank";
    
    var button = document.createElement("button");
    var x = document.createTextNode("\u274C");
    button.appendChild(x);
    button.onclick = function(event) {removeAttachment(event, div, a.guid);};
    button.style = "float: right";
    
    div.appendChild(anchor);
    div.appendChild(text);
    div.appendChild(button);
    div.onclick = function(event) { redirectClick(event, anchor);};
    div.className = "filelistitem";
    return div;
}

/**
 * User clicked outside of the anchor area, redirect it
 * to the anchor to view the document.
 * 
 * Do not redirect the delete button click.
 * 
 * @param {type} event
 * @param {type} anchor
 * @returns {undefined}
 */
function redirectClick(event, anchor) {
    if (event.target.nodeName !== "BUTTON") {
        anchor.click(); 
    }   
}

/**
 * User clicked the delete attachment button.
 * 
 * @param {type} event
 * @param {type} node
 * @param {type} attachGuid
 * @returns {undefined}
 */
async function removeAttachment(event, node, attachGuid) {
    console.log("Remove guid: " + attachGuid);
    const response = await fetch("upload/" + attachGuid, {'method': 'DELETE'});
    node.parentNode.removeChild(node);
}

async function removeTask(taskId) {
    if ((selectedTask.lId === "-1") || !selectedTask.lId) {
        // new task, has not been stored before.
        return;
    }
    
    const response = await fetch("system/removetask/" + selectedTask.lId, {'method': 'DELETE'});
    const responseData = await response.json();
    console.log(responseData);
}

/**
 * Load the list of attachment of this task
 * and render a selection list in the user
 * interface.
 * 
 * @returns {undefined}
 */
async function loadAttachments() {
    let filelist = document.getElementById("filelist");
    filelist.innerHTML = "";
    
    if ((selectedTask.lId === "-1") || !selectedTask.lId) {
        // new task, has not been stored before.
        return;
    }
    
    const response = await fetch("system/attachments/" + selectedTask.lId, {'method': 'GET'});
    const responseData = await response.json();
    console.log(responseData);

    var attachments = responseData.data;
    attachments.forEach((a) => {
        filelist.appendChild(makeAttachmentLink(a));
    });
}

/**
 * Writes an URL attachment into the database.
 * The created guid will be inserted into the
 * href of the newElementNode.
 * 
 * @param {string} name 
 * @param {string} url 
 * @param {Node} newElementNode 
 * @returns {undefined}
 */
async function saveUrlAttachment(name, url, newElementNode) {
    if ((selectedTask.lId === "-1") || !selectedTask.lId) {
        // new task, has not been stored before.
        return;
    }
    
    let postData = {}; 
    postData.taskId = selectedTask.lId;
    postData.fileName = name;
    postData.url = url;
    const response = await fetch("system/saveurl", {'method': 'POST', 'body': JSON.stringify(postData)});
    
    const responseData = await response.json();
    console.log(responseData);
    
    var guid = responseData.message;
    var anchor = newElementNode.childNodes[0];
    var href = anchor.href;
    href = href.replace("!!!placeholder!!!", guid);
    anchor.href = href;
}

function msgMissingSelection() {
    if (!selectedTask || selectedTask.lId === "-1") {
        statusMsg(Txt.selecttask);
        return true;
    }

    return false;
}

/**
 * Upload selected files to the server.
 * 
 * @returns {undefined}
 */
async function uploadFile() {
    if (msgMissingSelection()) {
        return;
    }
    
    let uploadfile = document.getElementById("uploadfile");
    let parent = uploadfile.parentNode;
    let filelist = document.getElementById("filelist");
    let formData = new FormData(); 
    let taskid = selectedTask.lId;
    formData.append("taskid", taskid);

    var newElements = [];
    for (var i = 0; i < uploadfile.files.length; i++) {
        formData.append("file", uploadfile.files[i]); 
        var name = uploadfile.files[i].name;
        var data = {};
        data.guid = "!!!placeholder!!!";
        data.fileName = name;
        var anchor = makeAttachmentLink(data);
        filelist.appendChild(anchor);
        newElements.push(anchor);
    }
    var newInput = document.createElement("input");
    newInput.type = "file";
    newInput.multiple = true;
    newInput.id=uploadfile.id;
    newInput.onchange = uploadfile.onchange;

    parent.replaceChild(newInput, uploadfile);
    const response = await fetch('upload', {
      method: "POST", 
      body: formData
    }); 

    // update guid in href
    const responseData = await response.json();
    console.log(responseData);
    for (var i = 0; i < newElements.length; i++) {
        var guid = responseData.data[i];
        var anchor = newElements[i].childNodes[0];
        var href = anchor.href;
        href = href.replace("!!!placeholder!!!", guid);
        anchor.href = href;
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
    document.getElementById("datelist").innerHTML = "";
    
    if (intervalData) {
        var intervalParts = intervalData.split("|");
        if (intervalParts.length === 3) {
            document.getElementById(intervalParts[0]).checked = true;
            setValue("divider", intervalParts[1]);
            var startDates = intervalParts[2].split(";");
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
 * @param {bool} isHistoryList 
 * @param {Map} visibleRoles 
 * @returns {undefined}
 */
function listTasks(tasklist, isHistoryList, visibleRoles) {
    var list = document.getElementById("tasklist");
    list.innerHTML = "";
    var td = toDay();
    var nextWeek = toDay(7);
    var futureBarInserted = false;
    var insertEscalationBar = false;
    
    tasklist.forEach((task) => {
        if (visibleRoles[task.owner] === false) {
            // filter from list
        } else {
            var newItem = document.createElement("div");
            newItem.className = "listitem";
            var taskName = document.createElement("div");
            taskName.className = "taskname";
            taskName.innerText = task.name;
            var dueDate = document.createElement("div");
            dueDate.innerText = task.nextDueDate;
            dueDate.className = "duedate";
            if (!isHistoryList) {
                if (task.nextDueDate < td) {
                    dueDate.classList.add("escalated");
                    insertEscalationBar = true;
                } else if (insertEscalationBar) {
                    insertEscalationBar = false;
                    bar = document.createElement("div");
                    bar.style = "height: 10pt;";
                    list.appendChild(bar);
                }
                if (task.nextDueDate > nextWeek) {
                    newItem.classList.add("futuretask");
                    dueDate.classList.add("futuretask");
                    if (!futureBarInserted) {
                        futureBarInserted = true;
                        bar = document.createElement("div");
                        bar.style = "height: 10pt;";
                        list.appendChild(bar);
                    }
                }
            }
        
            newItem.appendChild(taskName);
            newItem.appendChild(dueDate);
            newItem.userData = task;
            newItem.onclick = function() {fillForm(task);};
            list.appendChild(newItem);
        }
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
    dateField.style = "margin: 5pt;";
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
