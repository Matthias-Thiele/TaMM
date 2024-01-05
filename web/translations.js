/* 
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */

var TxtDe = {};
TxtDe.username = "Anwendername";
TxtDe.pasword = "Passwort";
TxtDe.pwdreq = "Passwort vergessen?";
TxtDe.wait = "Warte auf Serverantwort...";
TxtDe.dologin = "Anmelden";

var TxtFr = {};
TxtFr.username = "Nom d'utilisateur" ;
TxtFr.password = "Mot de passe" ;
TxtFr.pwdreq = "Mot de passe oublié ?";
TxtFr.wait = "En attente de réponse du serveur...";
TxtFr.dologin = "Registre";

var TxtEn = {};
TxtEn.username = "User name";
TxtEn.password = "Password";
TxtEn.pwdreq = "Password forgotten?";
TxtEn.wait = "Wait for server response...";
TxtEn.dologin = "Login";

var Txt;

function translateForm() {
    var userLang = navigator.language || navigator.userLanguage;
    switch (userLang) {
        case "de": Txt = TxtDe; break;
        case "fr": Txt = TxtFr; break;
        default: Txt = TxtEn;
    }
    
    if (userLang === "de") {
        return;
    }
    
    var textElems = document.querySelectorAll("[data-key]");
    textElems.forEach((elem) => {
        var key = elem.getAttribute("data-key");
        var translate = Txt[key];
        if (translate) {
            elem.innerText = translate;
        }
    });
}