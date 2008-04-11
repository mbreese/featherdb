package com.fourspaces.featherdb.views;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation describing what language this particular view handles.  There is a one-to-one relationship between handling classes and type values.
 * 
 * For example, only the JavaScriptView class handles the "text/javascript" view type.
 * 
 * @author mbreese
 *
 */

@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ViewType {
	String value();
}
