<%@ page buffer="none" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="cms" uri="http://www.opencms.org/taglib/cms" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!--DOCTYPE HTML-->
<html>
<head>
    <title>Testing</title>
    <cms:enable-ade />

</head>

<body>
<h1 style="text-align:center; padding-top: 1cm;">
    TESTING
</h1>
<cms:container name="centercontainer" type="element" width="500" maxElements="8" detailview="true" />

<cms:headincludes type="css" />
<cms:headincludes type="javascript" />
</body>
</html>