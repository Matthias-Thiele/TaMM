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
    
    loadAttachments();
}

/**
 * Create a new attachment download link element.
 * 
 * @param {type} a attachment data.
 * @returns {HTMLElement|makeAttachmentLink.anchor}
 */
function makeAttachmentLink(a) {
    var anchor = document.createElement("a");
    anchor.style = "text-decoration: none; color: black";
    var div = document.createElement("div");
    var text = document.createTextNode(a.fileName);
    div.appendChild(text);
    div.className = "filelistitem";
    anchor.appendChild(div);
    var url = "upload/" + a.guid + "/" + a.fileName;
    anchor.href = url;
    anchor.target = "_blank";
    return anchor;
}

/**
 * Load the list of attachment of this task
 * and render a selection list in the user
 * interface.
 * 
 * @returns {undefined}
 */
async function loadAttachments() {
    const response = await fetch("system/attachments/" + selectedTask.lId, {'method': 'GET'});
    const responseData = await response.json();
    console.log(responseData);

    let filelist = document.getElementById("filelist");
    filelist.innerHTML = "";
    
    var attachments = responseData.data;
    attachments.forEach((a) => {
        filelist.appendChild(makeAttachmentLink(a));
    });
}

/**
 * Upload selected files to the server.
 * 
 * @returns {undefined}
 */
async function uploadFile() {
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
      var href = newElements[i].href;
      href = href.replace("!!!placeholder!!!", guid);
      newElements[i].href = href;
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
