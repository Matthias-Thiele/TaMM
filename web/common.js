/* 
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
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
