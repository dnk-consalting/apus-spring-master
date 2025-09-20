<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@page contentType="text/html" pageEncoding="UTF-8" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
        <link rel="stylesheet" href="<%= request.getContextPath()%>/css/style.css" media="screen" /> 
        <title>${title}</title>

        <script language="javascript">

            var sec;
            var form;
            var counter;

            function $(id) {
                return document.getElementById(id);
            }

            window.onload = function () {
                sec = 9;
                form = $('form');
                counter = $('counter');
                window.setInterval(countDown, 1000);
            }

            function countDown() {
                if (sec < 0) {
                    var sid = $('session');
                    if (sid.value.length == 0) {
                        sid.disabled = true;
                    }
                    form.submit();
                } else {
                    counter.innerHTML = sec;
                }
                sec--;
            }

        </script>

    </head>
    <body>
        <table width="100%" height="100%" border="0">
            <tr>
                <td align="center" valign="middle">
                    <form action="${url}" method="get" id="form">
                        <h1>${i18n.E000001}</h1>
                        <br/>
                        <% if (request.getAttribute("type").equals(0)) {%>

                        <img src="<%= request.getContextPath()%>/images/ok.png" width="48" height="48"/>
                        <br/>
                        <br/>
                        <b>${i18n.E000002}</b>

                        <% } else if (request.getAttribute("type").equals(1)) {%>

                        <img src="<%= request.getContextPath()%>/images/error.png" width="48" height="48"/>
                        <br/>
                        <br/>
                        <b>${i18n.E000005}</b>


                        <% } else if (request.getAttribute("type").equals(2)) {%>
                        <img src="<%= request.getContextPath()%>/images/warning.png" width="48" height="48"/>
                        <br/>
                        <br/>
                        <b>${i18n.E000003}</b>
                        <% } else if (request.getAttribute("type").equals(3)) {%>
                        <img src="<%= request.getContextPath()%>/images/warning.png" width="48" height="48"/>
                        <br/>
                        <br/>
                        <b>${i18n.E000004}</b>
                        <% }%>
                        <br/>
                        <br/>
                        ${i18n.E000006}
                        <br/>
                        <br/>
                        <span><span id="counter">10</span> ${i18n.E000007}</span>
                        <br/>
                        <br/>
                        <input type="submit" value="${i18n.E000008}"/>
                        <input type="hidden" name="jsessionid" id="session" value="${session}"/>
                    </form>
                </td>
            </tr>
        </table>
    </body>
</html>