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

