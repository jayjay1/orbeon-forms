<?xml version="1.0" encoding="utf-8"?>
<!--
    Copyright (C) 2008 Orbeon, Inc.

    This program is free software; you can redistribute it and/or modify it under the terms of the
    GNU Lesser General Public License as published by the Free Software Foundation; either version
    2.1 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Lesser General Public License for more details.

    The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
-->
<!-- Return an aggregate so that each xbl:xbl can have its own metadata -->
<components xsl:version="2.0"
            xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
            xmlns:xs="http://www.w3.org/2001/XMLSchema"
            xmlns:xbl="http://www.w3.org/ns/xbl"
            xmlns:fb="http://orbeon.org/oxf/xml/form-builder"
            xmlns:xpl="java:org.orbeon.oxf.pipeline.api.FunctionLibrary">

    <xsl:variable name="app" as="xs:string" select="doc('input:parameters')/*/app"/>
    <xsl:variable name="form" as="xs:string" select="doc('input:parameters')/*/form"/>

    <!-- Find group names, e.g. "text", "selection", etc. -->
    <xsl:variable name="property-names" select="xpl:propertiesStartsWith('oxf.fb.toolbox.group')" as="xs:string*" />
    <xsl:variable name="unique-groups" select="distinct-values(for $v in $property-names return tokenize($v, '\.')[5])" as="xs:string*"/>

    <!-- Iterate over groups -->
    <xsl:for-each select="$unique-groups">

        <xsl:variable name="resources-property" select="xpl:property(string-join(('oxf.fb.toolbox.group', ., 'uri', $app, $form), '.'))" as="xs:string"/>
        <xsl:variable name="resources" select="for $uri in tokenize($resources-property, '\s+') return doc($uri)" as="document-node()*"/>

        <xsl:if test="$resources">
            <xbl:xbl>
                <!-- Add Form Builder metadata from first metadata found -->
                <xsl:variable name="metadata" select="($resources/*/fb:metadata)[1]" as="element(fb:metadata)?"/>
                <!-- TODO: what if missing? -->
                <xsl:copy-of select="$metadata"/>

                <!-- Copy all the scripts and bindings -->
                <xsl:for-each select="$resources">
                    <!-- XBL spec says script and binding can be in any order -->
                    <xsl:copy-of select="/*/(* except fb:metadata)"/>
                </xsl:for-each>
            </xbl:xbl>
        </xsl:if>

    </xsl:for-each>

    <!-- Global section templates -->
    <xsl:copy-of select="/xbl:xbl"/>
    <!-- Custom section templates (if different from "orbeon" as we don't want to copy components twice) -->
    <xsl:if test="$app != 'orbeon'">
        <xsl:copy-of select="doc('input:custom-template-xbl')/xbl:xbl"/>
    </xsl:if>
</components>