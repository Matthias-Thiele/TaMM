/* 
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */

/**
 * Returns the requested url parameter.
 * 
 * @param {type} name
 * @returns {String|null} Empty string if not found.
 */
function getUrlParam(name) {
    var url_string = window.location;
    var url = new URL(url_string);
    var c = url.searchParams.get(name);
    if (!c || (c === "undefined")) {
        c="";
    }
    return c;
}

/*
 * Send the logout command to the server and switch to the login page.
 */
async function logout(next) {
    const fetchlogout = await fetch("system/logout", {'method': 'GET'});
    const response = await fetchlogout.json();
    if (response.result === "ok") {
        console.log("Next page: " + response.nextPage + "?next=" + next);
        var np = response.nextPage;
        if (next) {
            np = np + "?next=" + next;
        }
        window.location = np;
    } else {
        // logout failed? why?
        statusMsg(response.message);
    }
}

/**
 * Initializes global behaviour.
 * @param {type} checkButtonId
 * @param {type} action 
 * @returns {undefined}
 */
function initDefaultAction(checkButtonId, action) {
    if (checkButtonId) {
        document.addEventListener("keypress", function (event) {
            if (event.keyCode === 13) {
                if (!document.getElementById(checkButtonId).disabled) {
                    action();
                }
            }
         });
    }
}

/**
 * Set the value property of a named element.
 * 
 * If the value is null or undefined then
 * insert an empty string.
 * 
 * @param {type} elementName
 * @param {type} value
 * @returns {undefined}
 */
function setValue(elementName, value) {
    if ((value === null) || (value === undefined)) {
        value = "";
    }

    document.getElementById(elementName).value = value;
}

/**
 * Reads the value of a named element.
 * 
 * @param {type} elementName
 * @returns {String|.document@call;getElementById.value}
 */
function getValue(elementName) {
    var value;
    var elem = document.getElementById(elementName);
    if (elem.type === "checkbox") {
        value = elem.checked ? "true" : "false";
    } else {
        value = elem.value;
        if (!value) {
            value = "";
        }
    }

    return value;
}

/**
 * Get all values of the nodes with the given name
 * and return them as one string with the given
 * seperator.
 * 
 * @param {type} name
 * @param {type} seperator
 * @returns {String}
 */
function getValues(name, seperator) {
    var result = [];
    var inputs = document.getElementsByName(name);
    inputs.forEach((input) => { 
        if (input.value) {
            result.push(input.value);
        }
    });

    return result.sort().join(seperator);
}

/**
 * Shows or hides a block element by id
 * 
 * @param {type} elementName
 * @param {type} doShow
 * @returns {undefined}
 */
function showBlockElement(elementName, doShow) {
  var x = document.getElementById(elementName);
  if (doShow) {
    x.style.display = "block";
  } else {
    x.style.display = "none";
  }
} 

/**
 * Actual date in ISO format.
 * 
 * @param {type} daysOffset offset from today in days
 * @returns {String}
 */
function toDay(daysOffset) {
    var dt = new Date();
    if (daysOffset) {
        dt.setDate(dt.getDate() + daysOffset);
    }
    
    return dt.toJSON().slice(0, 10);
}

/**
 * Replaces the T in a standard ISO date with a space.
 * 
 * @param {type} dateTime
 * @returns {String}
 */
function formatIso(dateTime) {
    if (dateTime) {
        var parts = dateTime.split("T");
        return parts.join(" ");
    } else {
        return "";
    }
}

/**
 * Writes a status message if the page has an Element
 * with the id status.
 * 
 * @param {type} message
 * @returns {undefined}
 */
function statusMsg(message) {
    var status = document.getElementById("status");
    if (status) {
        status.innerText = message;
    }
}

/**
 * Helper function for user selection.
 * @param {type} users
 * @returns {Userlist}
 */
function Userlist(users) {
    this.users = users;
}

/**
 * Creates the options list for the datalist tag.
 * This will be used by all user input fields.
 * @param {type} datalistId
 * @returns {undefined}
 */
Userlist.prototype.createOptionList = function(datalistId) {
    var list = document.getElementById(datalistId);
    list.innerHTML = "";
    this.users.forEach((idName) => {
        var option = document.createElement("option");
        option.value = idName.value;
        option.key = idName.key;
        list.appendChild(option);
    });
};

/**
 * Converts an user id into an user name.
 * @param {type} userName
 * @returns {Number}
 */
Userlist.prototype.idFromName = function(userName) {
    var result = -1;
    this.users.forEach((idName) => {
        if (idName.value === userName) {
            result = idName.key;
        }
    });

    return result;
};

/**
 * Converts an user name into an user id.
 * @param {type} userId
 * @returns {String}
 */
Userlist.prototype.nameFromId = function(userId) {
    var result = "";
    this.users.forEach((idName) => {
        if (idName.key === userId) {
            result = idName.value;
        }
    });

    return result;
};

/**
 * Creates a div tag with label text and input element.
 * 
 * @param {type} lineno
 * @param {type} linename
 * @param {type} labeltext
 * @param {type} isNumber
 * @param {type} text
 * @param {type} enabled 
 * @returns {createInputLine.outerDiv|HTMLElement}
 */
function createInputLine(lineno, linename, labeltext, isNumber, text, enabled) {
    var outerDiv = document.createElement("div");
    var label = document.createElement("label");
    label.innerText = labeltext;
    var id = (lineno >= 0) ? (linename + lineno) : linename;
    label.for = id;

    var input = document.createElement("input");
    input.id = id;
    input.disabled = !enabled;
    input.value = (text) ? text : "";
    if (isNumber) {
        input.type = "number";
        input.style = "width: 50pt;";
    }

    outerDiv.appendChild(label);
    outerDiv.appendChild(input);
    return outerDiv;
}
