/*
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */
package de.mmth.tamm.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 *
 * @author matthias
 */
public class ServletUtils {
  
public static String getClientIp(HttpServletRequest request) {
  String remoteAddr = "";

  if (request != null) {
    remoteAddr = request.getHeader("X-FORWARDED-FOR");
    if (remoteAddr == null || "".equals(remoteAddr)) {
        remoteAddr = request.getRemoteAddr();
    }
  }

  return remoteAddr;
}


}
