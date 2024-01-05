/* 
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */

            
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


function translateForm() {
    var userLang = navigator.language || navigator.userLanguage; userLang = "fr";
    if (userLang === "de") {
        return;
    }
    
    var Txt;
    switch (userLang) {
        case "fr": Txt = TxtFr; break;
        default: Txt = TxtEn;
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