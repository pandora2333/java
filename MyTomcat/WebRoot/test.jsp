<jsp langage="java">
<html>
   <head>
       <title>Welcome the Test Jsp||:#{username}|#{password}</title>
   </head>
    <body>
        <form action="index.html" method="post">
            <input type="text" name="#{user.username}">
            <input type="submit" value="#{user.password}">
        </form>
        #{user.username}
    <% System.out.println("hello world");%>
    </body>
</html>
</jsp>