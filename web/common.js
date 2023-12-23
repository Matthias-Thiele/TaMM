/* 
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */

/*
 * Send the logout command to the server and switch to the login page.
 */
function logout(next) {
    var request = new XMLHttpRequest();
    request.open("GET", "system/logout");
    request.setRequestHeader('Content-Type', 'application/json');
    request.send();
    request.overrideMimeType('application/json');
    request.onreadystatechange = function() {
        if (this.readyState === 4 && this.status === 200) {
            var response = JSON.parse(this.responseText);
            if (response.result === "ok") {
                console.log("Next page: " + response.nextPage + "?next=" + next);
                window.location = response.nextPage + "?next=" + next;
            } else {
                // logout failed? why?
            }
        }
    };
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

