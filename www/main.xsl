<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="http://www.w3.org/1999/xhtml"
  version="1.0" 
  xmlns:ni="xalan://org.apache.xalan.lib.NodeInfo"
  exclude-result-prefixes="ni">
  
  <xsl:output
    method="xml"
    indent="yes"
    encoding="utf-8"
    doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
    doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"/>

  <xsl:param name="basedir"/>
  <xsl:param name="branding"/>
  <xsl:variable name="brand" select="document(concat($branding,'/branding.xml'))/brand"/>
  <xsl:param name="webappprefix"/>
  <xsl:param name="outputext"/>
  <xsl:param name="sourceref"/>
<!--  <xsl:variable name="source" select="substring-after(ni:systemId(),concat($sourceref,'/'))"/>-->

  <xsl:include href="menu.xsl"/>
  <xsl:include href="rss.xsl"/>
  <xsl:include href="start.xsl"/>
  <xsl:include href="docbook.xsl"/>
  <xsl:include href="ulink.xsl"/>
  <xsl:include href="page_template.xsl"/>

  
  <xsl:template match="index">
    <ul>
      <xsl:apply-templates/>
    </ul>
  </xsl:template>
  
  <xsl:template match="section">
    <div class="box">
      <div class="heading2"><xsl:apply-templates select="title"/></div>
      <div class="body">
        <xsl:apply-templates select="*[position()!=1]"/>
      </div>
    </div>
  </xsl:template>
  
  <xsl:template match="column">
    <div style="width:50%;float:left">
      <xsl:apply-templates/>
    </div>
  </xsl:template>
  
 <!-- 
  <xsl:template match="section">
    <li><span><xsl:apply-templates select="heading"/></span><ul><xsl:apply-templates select="item"/></ul></li>
  </xsl:template>
  
  <xsl:template match="section/item">
    <li><xsl:apply-templates/></li>
  </xsl:template>
 --> 

  <xsl:template match="phrase">
    <address>
      <xsl:apply-templates/>
    </address>
  </xsl:template>
  
  <xsl:template mode="copy-no-ns" match="*">
    <xsl:element name="{name(.)}">
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates mode="copy-no-ns"/>
    </xsl:element>
  </xsl:template>
  
</xsl:stylesheet>
