package com.fourspaces.featherdb.views;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fourspaces.featherdb.document.Document;

/**
 * Annotation to tag a view for the types of documents that it will handle.  This is described by the implementing class, as opposed to the document's
 * mime-type.
 * 
 * @author mbreese
 *
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DocumentType {
	Class<? extends Document>[] value();
}
