<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- viewLine.jsp -->

        <c:forEach var="path" items="${viewList}" varStatus="status">
          <im:viewableDiv path="${path}" viewPaths="${viewPaths}" idPrefix="showing">
            <div>
                  <html:link action="/mainChange?method=changePath&amp;prefix=${viewPathLinkPrefixes[path]}&amp;path=${viewPathLinkPaths[viewPathLinkPrefixes[path]]}">
                    ${fn:replace(path, ".", " > ")}
                  </html:link>
            </div>
            <div>
              <span class="type"><small>${viewPathTypes[path]}</small></span>
            </div>
            <div style="white-space:nowrap">
              <c:choose>
                <c:when test="${status.first}">
                  <img style="margin-right: 5px" border="0" align="middle" 
                       src="images/blank13x13.gif" alt=" " width="13" height="13"/>
                </c:when>
                <c:otherwise>
                  <fmt:message key="view.moveLeftHelp" var="moveLeftTitle">
                    <fmt:param value="${path}"/>
                  </fmt:message>
                  <fmt:message key="view.moveLeftSymbol" var="moveLeftString"/>
                  
                      <html:link action="/viewChange?method=moveLeft&amp;index=${status.index}"
                             title="${moveLeftTitle}">
                        <img style="margin-right: 5px" border="0" align="middle" 
                            src="images/left-arrow-square.gif" width="13" height="13" 
                            alt="${moveRightString}"/>
                      </html:link>
                  
                </c:otherwise>
              </c:choose>                

              <fmt:message key="view.removeFromViewHelp" var="removeFromViewTitle">
                <fmt:param value="${path}"/>
              </fmt:message>
              <fmt:message key="view.removeFromViewSymbol" var="removeFromViewString"/>
              
                  <html:link action="/viewChange?method=removeFromView&amp;path=${path}"
                             title="${removeFromViewTitle}">
                    <img border="0" align="middle" 
                         src="images/cross.gif" width="13" height="13" 
                         alt="${removeFromViewString}"/>
                  </html:link>
                
              <c:choose>
                <c:when test="${status.last}">
                  <img style="margin-left: 5px" border="0" align="middle" 
                       src="images/blank13x13.gif" alt=" " width="13" height="13" />
                </c:when>
                <c:otherwise>
                  <fmt:message key="view.moveRightHelp" var="moveRightTitle">
                    <fmt:param value="${path}"/>
                  </fmt:message>
                  <fmt:message key="view.moveRightSymbol" var="moveRightString"/>
                  
                      <html:link action="/viewChange?method=moveRight&amp;index=${status.index}"
                                 title="${moveRightTitle}">
                        <img style="margin-left: 5px" border="0" align="middle"
                             src="images/right-arrow-square.gif" width="13" height="13" 
                             alt="${moveRightString}"/>
                      </html:link>
                      
                </c:otherwise>
              </c:choose>
            </div>
          </im:viewableDiv>
        </c:forEach>
      
<!-- viewLine.jsp -->

